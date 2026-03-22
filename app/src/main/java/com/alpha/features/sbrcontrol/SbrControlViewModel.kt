package com.alpha.features.sbrcontrol

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alpha.features.settings.AppSettings
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SbrControlViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    // ── UI State ─────────────────────────────────────────────────────────

    data class UiState(
        val btStatus: String = "Disconnected",
        val btConnecting: Boolean = false,
        val btConnected: Boolean = false,
        val fingers: List<Int> = emptyList(),
        val cmd: String = "S",
        val cmdByte: Int = 83,
        val smoothMode: Boolean = false,
        val transitionStatus: String = "",
        val landmarkerResult: HandLandmarkerResult? = null,
        val log: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Core components ───────────────────────────────────────────────────

    private val bt = BluetoothComm()
    private var stability = GestureStability(windowSize = AppSettings.DEFAULT_STABILITY_FRAMES)
    private var handLandmarker: HandLandmarker? = null
    private var transformationMatrix = Matrix()

    // ── Smooth mode state machine ─────────────────────────────────────────

    private var stabilityFrames = AppSettings.DEFAULT_STABILITY_FRAMES

    private enum class TransState { IDLE, COMPENSATE, STABILISE }
    private val opposite = mapOf("F" to "B", "B" to "F")
    private val motionGroup = mapOf(
        "F" to "linear", "B" to "linear", "S" to "linear",
        "L" to "rotational", "R" to "rotational",
        "O" to "rotational", "X" to "rotational"
    )

    private var transState = TransState.IDLE
    private var pendingCmd = ""
    private var compCmd = ""
    private var compTotal = 0
    private var compDone = 0
    private var stableCount = 0
    private var linearHold = 0
    private var prevCmd = ""

    // ── MediaPipe initialisation ──────────────────────────────────────────

    fun initLandmarker(context: android.content.Context) {
        if (handLandmarker != null) return
        viewModelScope.launch(Dispatchers.IO) {
            stabilityFrames = AppSettings.stabilityFramesFlow(ctx).first()
            stability = GestureStability(windowSize = stabilityFrames)

            try {
                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath("models/hand_landmarker.task")
                    .build()
                val options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setNumHands(1)
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener { result, _ -> processHandResult(result) }
                    .setErrorListener { e -> appendLog("MediaPipe error: ${e.message}") }
                    .build()
                handLandmarker = HandLandmarker.createFromOptions(context, options)
                appendLog("HandLandmarker ready (stability=$stabilityFrames frames)")
            } catch (e: Exception) {
                appendLog("ERROR: HandLandmarker init failed: ${e.message}")
            }
        }
    }

    // ── Bluetooth ─────────────────────────────────────────────────────────

    fun connectBluetooth() {
        viewModelScope.launch {
            val deviceName = AppSettings.btDeviceNameFlow(ctx).first()
            _uiState.update { it.copy(btStatus = "Connecting to $deviceName…", btConnecting = true) }
            val result = bt.connect(ctx, deviceName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(btStatus = "Connected ● $deviceName", btConnected = true, btConnecting = false) }
                    appendLog("BT connected to $deviceName")
                },
                onFailure = { e ->
                    _uiState.update { it.copy(btStatus = "Failed: ${e.message}", btConnected = false, btConnecting = false) }
                    appendLog("BT error: ${e.message}")
                }
            )
        }
    }

    fun disconnectBluetooth() {
        bt.disconnect()
        _uiState.update { it.copy(btStatus = "Disconnected", btConnected = false, btConnecting = false) }
        appendLog("BT disconnected")
    }

    fun toggleSmoothMode() {
        linearHold = 0
        resetTransition()
        _uiState.update { it.copy(smoothMode = !it.smoothMode) }
        appendLog("Mode → ${if (_uiState.value.smoothMode) "SMOOTH" else "DIRECT"}")
    }

    // ── Frame processing ──────────────────────────────────────────────────

    private var isProcessing = false

    fun processFrame(imageProxy: ImageProxy) {
        val landmarker = handLandmarker
        if (landmarker == null || isProcessing) { imageProxy.close(); return }
        
        isProcessing = true
        try {
            val bitmap   = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees.toFloat()
            transformationMatrix.reset()
            transformationMatrix.postRotate(rotation)
            val rotated  = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transformationMatrix, true)
            val mpImage  = BitmapImageBuilder(rotated).build()
            landmarker.detectAsync(mpImage, System.currentTimeMillis())
        } catch (e: Exception) {
            appendLog("Frame error: ${e.message}")
            isProcessing = false
        } finally {
            imageProxy.close()
        }
    }

    private fun processHandResult(result: HandLandmarkerResult) {
        try {
            val gestureCmd: String
            val fingers: List<Int>

            if (result.landmarks().isNotEmpty() && result.handedness().isNotEmpty()) {
                val landmarks = result.landmarks()[0]
                val rawLabel  = result.handedness()[0][0].categoryName()
                fingers       = HandGestureProcessor.getFingers(landmarks, rawLabel)
                val cmd       = GestureLogic.fingersToCmd(fingers)
                gestureCmd    = stability.update(cmd)
            } else {
                fingers    = emptyList()
                gestureCmd = stability.update("S")
            }

            when {
                gestureCmd in listOf("F", "B") ->
                    linearHold = if (gestureCmd == prevCmd) minOf(linearHold + 1, 278) else 1
                gestureCmd == "S" -> linearHold = maxOf(linearHold - 1, 0)
                else -> linearHold = 0
            }

            val byte = dispatchCommand(gestureCmd)

            _uiState.update { 
                it.copy(
                    fingers          = fingers,
                    cmd              = gestureCmd,
                    cmdByte          = byte,
                    landmarkerResult = result,
                    transitionStatus = transitionStatusString()
                )
            }
        } finally {
            isProcessing = false
        }
    }

    // ── Command dispatch ──────────────────────────────────────────────────

    private fun dispatchCommand(gestureCmd: String): Int {
        if (!bt.isConnected) { prevCmd = gestureCmd; return GestureLogic.cmdToByte(gestureCmd) }
        return if (!_uiState.value.smoothMode) {
            if (gestureCmd != prevCmd) {
                val b = GestureLogic.cmdToByte(gestureCmd)
                bt.sendByte(b); prevCmd = gestureCmd; b
            } else GestureLogic.cmdToByte(prevCmd)
        } else dispatchSmooth(gestureCmd)
    }

    private fun dispatchSmooth(gestureCmd: String): Int {
        when (transState) {
            TransState.IDLE -> {
                if (gestureCmd != prevCmd) {
                    if (crossGroup(prevCmd, gestureCmd)) {
                        pendingCmd = gestureCmd
                        val opp = opposite[prevCmd]
                        if (opp != null && linearHold > 0) {
                            transState = TransState.COMPENSATE; compCmd = opp
                            compTotal = linearHold; compDone = 0
                            bt.sendByte(GestureLogic.cmdToByte(compCmd)); prevCmd = compCmd
                            return GestureLogic.cmdToByte(compCmd)
                        } else {
                            transState = TransState.STABILISE; stableCount = 0
                            bt.sendByte(GestureLogic.cmdToByte("S")); prevCmd = "S"
                            return GestureLogic.cmdToByte("S")
                        }
                    } else {
                        val b = GestureLogic.cmdToByte(gestureCmd)
                        bt.sendByte(b); prevCmd = gestureCmd; return b
                    }
                }
            }
            TransState.COMPENSATE -> {
                compDone++
                return if (compDone < compTotal) {
                    bt.sendByte(GestureLogic.cmdToByte(compCmd)); GestureLogic.cmdToByte(compCmd)
                } else {
                    transState = TransState.STABILISE; stableCount = 0
                    bt.sendByte(GestureLogic.cmdToByte("S")); prevCmd = "S"
                    GestureLogic.cmdToByte("S")
                }
            }
            TransState.STABILISE -> {
                bt.sendByte(GestureLogic.cmdToByte("S"))
                if (gestureCmd == pendingCmd) stableCount++
                else { pendingCmd = gestureCmd; stableCount = 1 }
                if (stableCount >= stabilityFrames) {
                    val b = GestureLogic.cmdToByte(pendingCmd)
                    bt.sendByte(b); prevCmd = pendingCmd; linearHold = 0
                    resetTransition(); return b
                }
                return GestureLogic.cmdToByte("S")
            }
        }
        return GestureLogic.cmdToByte(prevCmd)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun crossGroup(a: String, b: String) = motionGroup[a] != motionGroup[b]

    private fun resetTransition() {
        transState = TransState.IDLE; pendingCmd = ""; compCmd = ""
        compTotal = 0; compDone = 0; stableCount = 0
    }

    private fun transitionStatusString() = when (transState) {
        TransState.COMPENSATE -> "compensating $compDone/$compTotal"
        TransState.STABILISE  -> "stabilising $stableCount/$stabilityFrames"
        TransState.IDLE       -> ""
    }

    private fun appendLog(msg: String) {
        _uiState.update { 
            val lines = it.log.lines().takeLast(8) + msg
            it.copy(log = lines.joinToString("\n"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        bt.disconnect()
        handLandmarker?.close()
    }
}
