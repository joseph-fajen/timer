package com.igygtimer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun `formatTime returns 0 colon 00 for zero millis`() {
        assertEquals("0:00", TimeUtils.formatTime(0))
    }

    @Test
    fun `formatTime formats seconds correctly`() {
        assertEquals("0:05", TimeUtils.formatTime(5000))
        assertEquals("0:59", TimeUtils.formatTime(59000))
    }

    @Test
    fun `formatTime formats minutes and seconds correctly`() {
        assertEquals("1:00", TimeUtils.formatTime(60000))
        assertEquals("1:30", TimeUtils.formatTime(90000))
        assertEquals("10:05", TimeUtils.formatTime(605000))
    }

    @Test
    fun `formatTime handles large values`() {
        assertEquals("60:00", TimeUtils.formatTime(3600000))
    }

    @Test
    fun `calculateRestDuration with ratio 1 returns same duration`() {
        assertEquals(30000L, TimeUtils.calculateRestDuration(30000, 1.0f))
    }

    @Test
    fun `calculateRestDuration with ratio 2 doubles duration`() {
        assertEquals(60000L, TimeUtils.calculateRestDuration(30000, 2.0f))
    }

    @Test
    fun `calculateRestDuration with ratio 0 point 5 halves duration`() {
        assertEquals(15000L, TimeUtils.calculateRestDuration(30000, 0.5f))
    }
}
