package com.alpha.features.sbrcontrol

/**
 * GestureLogic.kt
 *
 * Port of gesture_logic.py (V7) fingers_to_cmd().
 *
 * The SUM of the finger array is the key — order does not matter.
 *
 *   sum=0 → "F"  Forward
 *   sum=1 → "B"  Backward
 *   sum=2 → "L"  Turn Left
 *   sum=3 → "R"  Turn Right
 *   sum=4 → "O"  Rotate CW
 *   sum=5 → "X"  Rotate CCW
 *   else  → "S"  Stop (safety fallback)
 */
object GestureLogic {

    private val gestureMap = mapOf(
        0 to "F",
        1 to "B",
        2 to "L",
        3 to "R",
        4 to "O",
        5 to "X"
    )

    val cmdBytes = mapOf(
        "F" to 70,
        "B" to 66,
        "L" to 76,
        "R" to 82,
        "O" to 79,
        "X" to 88,
        "S" to 83
    )

    fun fingersToCmd(fingers: List<Int>): String {
        val count = fingers.sum()
        return gestureMap[count] ?: "S"
    }

    fun cmdToByte(cmd: String): Int = cmdBytes[cmd] ?: 83
}
