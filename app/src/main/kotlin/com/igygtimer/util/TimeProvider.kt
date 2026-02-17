package com.igygtimer.util

import android.os.SystemClock

/**
 * Abstraction for time source to enable testing.
 * In production, uses SystemClock.uptimeMillis() for drift-free timing.
 * In tests, can be replaced with a fake implementation.
 */
interface TimeProvider {
    fun uptimeMillis(): Long
}

/**
 * Default implementation using Android's SystemClock.
 * This is monotonic and not affected by wall clock changes.
 */
class SystemTimeProvider : TimeProvider {
    override fun uptimeMillis(): Long = SystemClock.uptimeMillis()
}
