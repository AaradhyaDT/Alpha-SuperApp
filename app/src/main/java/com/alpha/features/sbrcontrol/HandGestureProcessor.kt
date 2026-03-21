package com.alpha.features.sbrcontrol

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

/**
 * HandGestureProcessor.kt
 *
 * Port of ml_gesture.py → get_fingers() (V7).
 *
 * MediaPipe Android's HandLandmarker returns List<NormalizedLandmark>
 * with the same 21 indices as the Python version.
 *
 * Landmark indices used:
 *   [2]  Thumb MCP     [3]  Thumb IP     [4]  Thumb tip
 *   [6]  Index PIP     [8]  Index tip
 *   [10] Middle PIP    [12] Middle tip
 *   [14] Ring PIP      [16] Ring tip
 *   [18] Pinky PIP     [20] Pinky tip
 *
 * rawLabel: MediaPipe's handedness string ("Left" or "Right") BEFORE
 *           any selfie-view mirroring adjustment. This is geometrically
 *           correct for the x-axis thumb check — same as Python version.
 *
 * Only the SUM of the returned list matters for command mapping.
 */
object HandGestureProcessor {

    /**
     * Returns [thumb, index, middle, ring, pinky] — 1 = extended, 0 = folded.
     *
     * @param lm        21 NormalizedLandmark objects from MediaPipe HandLandmarker
     * @param rawLabel  MediaPipe handedness label: "Left" or "Right"
     */
    fun getFingers(lm: List<NormalizedLandmark>, rawLabel: String): List<Int> {
        val fingers = mutableListOf<Int>()

        // ---- THUMB ----
        // x-axis: direction depends on which hand MediaPipe thinks it is
        //   "Right" hand (MediaPipe) → thumb is on the LEFT of image → extended: tip.x < IP.x
        //   "Left"  hand (MediaPipe) → thumb is on the RIGHT of image → extended: tip.x > IP.x
        // y-axis: tip must be above MCP base (not tucked downward)
        val thumbXOk = if (rawLabel == "Right") {
            lm[4].x() < lm[3].x()
        } else {
            lm[4].x() > lm[3].x()
        }
        val thumbYOk = lm[4].y() < lm[2].y()
        fingers.add(if (thumbXOk && thumbYOk) 1 else 0)

        // ---- FOUR FINGERS — vertical tip-vs-PIP test ----
        fingers.add(if (lm[8].y()  < lm[6].y())  1 else 0)  // index
        fingers.add(if (lm[12].y() < lm[10].y()) 1 else 0)  // middle
        fingers.add(if (lm[16].y() < lm[14].y()) 1 else 0)  // ring
        fingers.add(if (lm[20].y() < lm[18].y()) 1 else 0)  // pinky

        return fingers
    }
}
