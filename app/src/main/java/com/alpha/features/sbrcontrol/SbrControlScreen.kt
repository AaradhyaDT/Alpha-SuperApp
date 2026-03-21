package com.alpha.features.sbrcontrol

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpha.ui.theme.StatusGreen
import com.alpha.ui.theme.StatusRed
import java.util.concurrent.Executors

// Hardcoded private color block removed — all colors now come from
// MaterialTheme.colorScheme so both light and dark modes work correctly.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SbrControlScreen(
    onBack: () -> Unit,
    vm: SbrControlViewModel = viewModel()
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state          by vm.uiState.collectAsState()

    // ── Colour aliases — resolved at composition time from active theme ───
    // Replaces: BgDark, CardSurface, Purple, PurpleLight, White, Green, Red
    val bgColor      = MaterialTheme.colorScheme.background        // was BgDark
    val cardColor    = MaterialTheme.colorScheme.surface           // was CardSurface
    val primary      = MaterialTheme.colorScheme.primary           // was Purple
    val onCard       = MaterialTheme.colorScheme.onSurface         // was White
    val subtle       = MaterialTheme.colorScheme.onSurfaceVariant  // was PurpleLight
    val dividerColor = MaterialTheme.colorScheme.outline           // was Purple.copy(alpha=0.3f)

    // StatusGreen / StatusRed are semantic fixed colors — theme-independent
    // (imported from Color.kt — were Green / Red hardcoded locally)

    // ── Permission handling ───────────────────────────────────────────────
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var btGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        cameraGranted = grants[Manifest.permission.CAMERA] == true
        btGranted     = grants[Manifest.permission.BLUETOOTH_CONNECT] == true
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        )
        vm.initLandmarker(context)
    }

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SBR Control",
                        // was: color = White
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            // was: tint = White
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    // was: containerColor = BgDark
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        // was: containerColor = BgDark
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Camera preview ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    // was: .background(CardSurface)
                    .background(cardColor)
            ) {
                if (cameraGranted) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                                            vm.processFrame(imageProxy)
                                        }
                                    }
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // was: color = PurpleLight
                        Text("Camera permission required", color = subtle)
                    }
                }

                // CMD overlay
                if (state.cmd.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            // was: CardSurface.copy(alpha = 0.75f)
                            .background(cardColor.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cmdLabel(state.cmd),
                            // was: color = White
                            color = onCard,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Status panel ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    // was: .background(CardSurface, ...)
                    .background(cardColor, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // BT status row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // StatusGreen / StatusRed: fixed semantic colors, unchanged
                    val dotColor = if (state.btConnected) StatusGreen else StatusRed
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, RoundedCornerShape(5.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    // was: color = White
                    Text(state.btStatus, color = onCard, fontSize = 13.sp)
                }

                // was: color = Purple.copy(alpha = 0.3f)
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                StatusRow(
                    label = "Fingers",
                    value = if (state.fingers.isEmpty()) "—"
                    else state.fingers.joinToString(", ", "[", "]"),
                    // labelColor / valueColor now flow from theme inside StatusRow
                )

                StatusRow(
                    label = "CMD",
                    value = "${state.cmd}  →  byte: ${state.cmdByte}"
                )

                if (state.smoothMode && state.transitionStatus.isNotEmpty()) {
                    StatusRow(
                        label = "Trans",
                        value = state.transitionStatus,
                        // was: valueColor = PurpleLight
                        valueColor = subtle
                    )
                }

                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                // Mode toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Mode: ${if (state.smoothMode) "SMOOTH" else "DIRECT"}",
                        // was: color = PurpleLight
                        color = subtle,
                        fontSize = 12.sp
                    )
                    Switch(
                        checked = state.smoothMode,
                        onCheckedChange = { vm.toggleSmoothMode() },
                        colors = SwitchDefaults.colors(
                            // was: checkedThumbColor = Purple
                            checkedThumbColor = primary,
                            // was: checkedTrackColor = Purple.copy(alpha = 0.4f)
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── BT connect / disconnect buttons ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { vm.connectBluetooth() },
                    enabled = !state.btConnected && btGranted,
                    colors = ButtonDefaults.buttonColors(
                        // was: containerColor = Purple
                        containerColor = primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                        // Disabled state — visible in both themes
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Connect BT", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { vm.disconnectBluetooth() },
                    enabled = state.btConnected,
                    colors = ButtonDefaults.outlinedButtonColors(
                        // was: contentColor = PurpleLight — caused invisible text in light mode
                        contentColor          = MaterialTheme.colorScheme.primary,
                        disabledContentColor  = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        // Tints the outline with primary so it's visible in both themes
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (state.btConnected) primary
                            else MaterialTheme.colorScheme.outline
                        )
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect", fontSize = 13.sp)
                }
            }

            // ── Log strip ─────────────────────────────────────────────────
            if (state.log.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        // was: .background(CardSurface, ...)
                        .background(cardColor, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = state.log,
                        // was: color = PurpleLight
                        color = subtle,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun StatusRow(
    label: String,
    value: String,
    // was: valueColor: Color = White — hardcoded, broke in light mode
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // was: color = PurpleLight
        Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(value, color = valueColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun cmdLabel(cmd: String): String = when (cmd) {
    "F"  -> "▲ FORWARD"
    "B"  -> "▼ BACKWARD"
    "L"  -> "◄ LEFT"
    "R"  -> "► RIGHT"
    "O"  -> "↻ ROTATE CW"
    "X"  -> "↺ ROTATE CCW"
    "S"  -> "■ STOP"
    else -> cmd
}