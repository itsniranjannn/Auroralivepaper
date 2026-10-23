package com.niranjan.auroralivepaper

import com.niranjan.auroralivepaper.util.RecoveryLimiter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecoveryLimiterTest {

    private lateinit var limiter: RecoveryLimiter

    @Before
    fun setUp() {
        limiter = RecoveryLimiter()
    }

    @Test
    fun testBackoffSequenceProgression() {
        var time = 1000L

        // 1st recovery: 1000ms delay
        val res1 = limiter.attemptRecovery(time)
        assertTrue(res1 is RecoveryLimiter.Result.Scheduled)
        assertEquals(1000L, (res1 as RecoveryLimiter.Result.Scheduled).delayMs)
        limiter.onRecoveryDispatched()

        // 2nd recovery: 2000ms delay
        time += 5000L
        val res2 = limiter.attemptRecovery(time)
        assertTrue(res2 is RecoveryLimiter.Result.Scheduled)
        assertEquals(2000L, (res2 as RecoveryLimiter.Result.Scheduled).delayMs)
        limiter.onRecoveryDispatched()

        // 3rd recovery: 4000ms delay
        time += 5000L
        val res3 = limiter.attemptRecovery(time)
        assertTrue(res3 is RecoveryLimiter.Result.Scheduled)
        assertEquals(4000L, (res3 as RecoveryLimiter.Result.Scheduled).delayMs)
        limiter.onRecoveryDispatched()

        // 4th recovery: 8000ms delay
        time += 5000L
        val res4 = limiter.attemptRecovery(time)
        assertTrue(res4 is RecoveryLimiter.Result.Scheduled)
        assertEquals(8000L, (res4 as RecoveryLimiter.Result.Scheduled).delayMs)
        limiter.onRecoveryDispatched()

        // 5th recovery: 30000ms delay
        time += 5000L
        val res5 = limiter.attemptRecovery(time)
        assertTrue(res5 is RecoveryLimiter.Result.Scheduled)
        assertEquals(30000L, (res5 as RecoveryLimiter.Result.Scheduled).delayMs)
        limiter.onRecoveryDispatched()

        // 6th recovery in the same 2-min window: should be Suppressed
        time += 5000L
        val res6 = limiter.attemptRecovery(time)
        assertEquals(RecoveryLimiter.Result.Suppressed, res6)
        assertTrue(limiter.isRecoverySuppressed)
    }

    @Test
    fun testAlreadyScheduledGuard() {
        val res1 = limiter.attemptRecovery(1000L)
        assertTrue(res1 is RecoveryLimiter.Result.Scheduled)

        // Attempting again before dispatch should return AlreadyScheduled
        val res2 = limiter.attemptRecovery(1500L)
        assertEquals(RecoveryLimiter.Result.AlreadyScheduled, res2)

        limiter.onRecoveryDispatched()
        assertFalse(limiter.isRecoveryScheduled)
    }

    @Test
    fun testSlidingWindowEviction() {
        // Dispatch 5 recoveries at t=0, 10s, 20s, 30s, 40s
        var t = 1000L
        for (i in 1..5) {
            val r = limiter.attemptRecovery(t)
            assertTrue(r is RecoveryLimiter.Result.Scheduled)
            limiter.onRecoveryDispatched()
            t += 10_000L
        }

        // At t = 50s, 6th attempt within 120s window is suppressed
        val supp = limiter.attemptRecovery(t)
        assertEquals(RecoveryLimiter.Result.Suppressed, supp)

        // Reset called on user action / visibility change
        limiter.reset()
        assertFalse(limiter.isRecoverySuppressed)

        // Advance beyond window (e.g. 150 seconds later)
        t += 150_000L
        val allowed = limiter.attemptRecovery(t)
        assertTrue(allowed is RecoveryLimiter.Result.Scheduled)
    }
}
