package com.igygtimer.util

/**
 * Fake TimeProvider for unit tests.
 * Allows manual control of time progression.
 */
class FakeTimeProvider(
    private var currentTimeMs: Long = 0L
) : TimeProvider {

    override fun uptimeMillis(): Long = currentTimeMs

    fun advanceBy(millis: Long) {
        currentTimeMs += millis
    }

    fun setTime(millis: Long) {
        currentTimeMs = millis
    }
}
