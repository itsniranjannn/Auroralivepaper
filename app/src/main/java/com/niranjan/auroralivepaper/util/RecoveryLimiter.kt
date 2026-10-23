package com.niranjan.auroralivepaper.util

/**
 * Pure Kotlin rate limiter and backoff scheduler for player recovery.
 * Contains no Android framework dependencies so it can be cleanly unit-tested.
 */
class RecoveryLimiter(
    private val maxRecoveries: Int = 5,
    private val windowDurationMs: Long = 120_000L,
    private val backoffSequence: List<Long> = listOf(1000L, 2000L, 4000L, 8000L, 30000L)
) {
    private val timestamps = ArrayDeque<Long>()
    private var isScheduled = false
    private var isSuppressed = false
    private var backoffIndex = 0

    val isRecoveryScheduled: Boolean
        get() = isScheduled

    val isRecoverySuppressed: Boolean
        get() = isSuppressed

    sealed class Result {
        data class Scheduled(val delayMs: Long) : Result()
        object AlreadyScheduled : Result()
        object Suppressed : Result()
    }

    /**
     * Attempts to schedule a recovery operation.
     * @param currentTimeMs Current time in milliseconds (e.g. SystemClock.uptimeMillis())
     * @return [Result] indicating whether recovery is scheduled, already scheduled, or suppressed.
     */
    fun attemptRecovery(currentTimeMs: Long): Result {
        if (isScheduled) {
            return Result.AlreadyScheduled
        }

        // Evict timestamps older than the sliding window
        while (timestamps.isNotEmpty() && (currentTimeMs - timestamps.first() > windowDurationMs)) {
            timestamps.removeFirst()
        }

        if (isSuppressed || timestamps.size >= maxRecoveries) {
            isSuppressed = true
            return Result.Suppressed
        }

        timestamps.addLast(currentTimeMs)
        isScheduled = true

        val delay = backoffSequence[backoffIndex.coerceAtMost(backoffSequence.size - 1)]
        if (backoffIndex < backoffSequence.size - 1) {
            backoffIndex++
        }

        return Result.Scheduled(delay)
    }

    /**
     * Mark the currently scheduled recovery as finished / dispatched.
     */
    fun onRecoveryDispatched() {
        isScheduled = false
    }

    /**
     * Fully reset limiter state (called on visibility resume, new media, etc.).
     */
    fun reset() {
        timestamps.clear()
        isScheduled = false
        isSuppressed = false
        backoffIndex = 0
    }

    /**
     * Reset backoff ladder upon successful player preparation.
     */
    fun resetBackoff() {
        backoffIndex = 0
    }
}
