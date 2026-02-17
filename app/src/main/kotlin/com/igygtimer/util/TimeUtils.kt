package com.igygtimer.util

object TimeUtils {
    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun calculateRestDuration(workDurationMs: Long, ratio: Float): Long {
        return (workDurationMs * ratio).toLong()
    }
}
