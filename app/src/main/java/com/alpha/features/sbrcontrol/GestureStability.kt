package com.alpha.features.sbrcontrol

/**
 * GestureStability.kt
 *
 * Direct port of gesture_stability.py (V7).
 *
 * Requires [windowSize] consecutive identical commands before confirming
 * a new one. Prevents jitter from borderline finger detections.
 *
 * update() always returns the last confirmed command — callers never
 * receive null and never silently drop a gesture. Default confirmed = "S".
 */
class GestureStability(private val windowSize: Int = 6) {

    private val buffer = ArrayDeque<String>(windowSize)
    var confirmed: String = "S"
        private set

    fun update(cmd: String): String {
        if (buffer.size >= windowSize) buffer.removeFirst()
        buffer.addLast(cmd)

        if (buffer.size == windowSize && buffer.all { it == cmd }) {
            confirmed = cmd
        }
        return confirmed
    }

    fun reset() {
        buffer.clear()
        confirmed = "S"
    }
}
