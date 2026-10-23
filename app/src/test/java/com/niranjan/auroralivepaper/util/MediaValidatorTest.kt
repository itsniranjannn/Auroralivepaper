package com.niranjan.auroralivepaper.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaValidatorTest {

    @Test
    fun testDefaultAndZeroValues_produceNoWarnings() {
        val warnings = MediaValidator.evaluate()
        assertTrue(warnings.isEmpty())

        val zeroWarnings = MediaValidator.evaluate(
            width = 0,
            height = 0,
            fps = 0f,
            durationMs = 0L,
            sizeBytes = 0L
        )
        assertTrue(zeroWarnings.isEmpty())
    }

    @Test
    fun testResolutionThresholds() {
        // Boundary at 1920
        val normalRes = MediaValidator.evaluate(width = 1920, height = 1080)
        assertTrue(normalRes.isEmpty())

        // Exceeding 1920 on width
        val highWidth = MediaValidator.evaluate(width = 1921, height = 1080)
        assertEquals(1, highWidth.size)
        assertTrue(highWidth[0].contains("Resolution is high"))

        // Exceeding 1920 on height
        val highHeight = MediaValidator.evaluate(width = 1080, height = 1921)
        assertEquals(1, highHeight.size)
        assertTrue(highHeight[0].contains("Resolution is high"))
    }

    @Test
    fun testFpsThresholds() {
        // Boundary at 30 fps
        val normalFps = MediaValidator.evaluate(fps = 30f)
        assertTrue(normalFps.isEmpty())

        // Exceeding 30 fps
        val highFps = MediaValidator.evaluate(fps = 30.1f)
        assertEquals(1, highFps.size)
        assertTrue(highFps[0].contains("Frame rate is high"))

        val fps60 = MediaValidator.evaluate(fps = 60f)
        assertEquals(1, fps60.size)
        assertTrue(fps60[0].contains("Frame rate is high"))
    }

    @Test
    fun testDurationThresholds() {
        // Boundary at 60s (60,000 ms)
        val normalDuration = MediaValidator.evaluate(durationMs = 60_000L)
        assertTrue(normalDuration.isEmpty())

        // Exceeding 60s
        val longDuration = MediaValidator.evaluate(durationMs = 60_001L)
        assertEquals(1, longDuration.size)
        assertTrue(longDuration[0].contains("Duration is long"))
    }

    @Test
    fun testSizeBytesThresholds() {
        val maxSizeBytes = 150L * 1024L * 1024L // 150 MB

        // Boundary at 150 MB
        val normalSize = MediaValidator.evaluate(sizeBytes = maxSizeBytes)
        assertTrue(normalSize.isEmpty())

        // Exceeding 150 MB
        val largeSize = MediaValidator.evaluate(sizeBytes = maxSizeBytes + 1L)
        assertEquals(1, largeSize.size)
        assertTrue(largeSize[0].contains("File size is large"))
    }

    @Test
    fun testMultipleWarningsTriggered() {
        val warnings = MediaValidator.evaluate(
            width = 3840,
            height = 2160,
            fps = 60f,
            durationMs = 120_000L,
            sizeBytes = 200L * 1024L * 1024L
        )
        assertEquals(4, warnings.size)
    }
}
