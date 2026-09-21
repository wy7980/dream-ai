package com.example.data.api

import com.example.data.model.RateLimitState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.min

class RateLimitManager(
    private var cooldownIntervalSeconds: Int = 60
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    private var lastRequestTimestamp: Long = 0L

    private val _rateLimitState = MutableStateFlow(
        RateLimitState(
            isCoolingDown = false,
            remainingSeconds = 0,
            totalCooldownSeconds = cooldownIntervalSeconds,
            lastCallTime = 0L,
            pendingQueueCount = 0,
            currentExecutingTask = null
        )
    )
    val rateLimitState: StateFlow<RateLimitState> = _rateLimitState.asStateFlow()

    init {
        // Background ticker for live UI countdowns
        scope.launch {
            while (true) {
                delay(1000L)
                updateTicker()
            }
        }
    }

    fun updateCooldownInterval(seconds: Int) {
        cooldownIntervalSeconds = seconds
        _rateLimitState.update { it.copy(totalCooldownSeconds = seconds) }
    }

    private fun updateTicker() {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastRequestTimestamp) / 1000L
        val remaining = max(0L, cooldownIntervalSeconds - elapsed).toInt()

        _rateLimitState.update { current ->
            current.copy(
                isCoolingDown = remaining > 0 && lastRequestTimestamp > 0,
                remainingSeconds = remaining
            )
        }
    }

    suspend fun <T> executeRateLimited(
        taskName: String,
        block: suspend () -> T
    ): T {
        _rateLimitState.update { it.copy(pendingQueueCount = it.pendingQueueCount + 1) }

        return try {
            mutex.withLock {
                _rateLimitState.update {
                    it.copy(
                        currentExecutingTask = taskName,
                        pendingQueueCount = max(0, it.pendingQueueCount - 1)
                    )
                }

                // Check remaining cooldown
                val now = System.currentTimeMillis()
                val elapsedSeconds = (now - lastRequestTimestamp) / 1000L
                val waitSeconds = (cooldownIntervalSeconds - elapsedSeconds).toInt()

                if (waitSeconds > 0 && lastRequestTimestamp > 0) {
                    var remaining = waitSeconds
                    while (remaining > 0) {
                        _rateLimitState.update {
                            it.copy(
                                isCoolingDown = true,
                                remainingSeconds = remaining,
                                currentExecutingTask = "$taskName (限速冷却等待中 $remaining 秒...)"
                            )
                        }
                        delay(1000L)
                        remaining--
                    }
                }

                _rateLimitState.update {
                    it.copy(
                        isCoolingDown = false,
                        remainingSeconds = 0,
                        currentExecutingTask = "$taskName (正在调用 Agnes API...)"
                    )
                }

                // Mark timestamp right before API call
                lastRequestTimestamp = System.currentTimeMillis()
                _rateLimitState.update { it.copy(lastCallTime = lastRequestTimestamp) }

                val result = block()

                result
            }
        } finally {
            _rateLimitState.update {
                it.copy(currentExecutingTask = null)
            }
        }
    }

    fun getRemainingSeconds(): Int {
        val now = System.currentTimeMillis()
        val elapsed = (now - lastRequestTimestamp) / 1000L
        return max(0L, cooldownIntervalSeconds - elapsed).toInt()
    }

    /**
     * Runs [block] inside the rate limiter, retrying with exponential backoff when the server
     * signals a transient rate limit (HTTP 429 / `rate_limit_exceeded`). [block] must throw
     * [RateLimitException] for a retryable response and any other exception to fail fast.
     */
    suspend fun <T> executeRateLimitedWithRetry(
        taskName: String,
        maxAttempts: Int = 6,
        baseDelayMs: Long = 5_000L,
        block: suspend (attempt: Int) -> T
    ): T {
        var attempt = 1
        while (true) {
            try {
                return executeRateLimited(taskName) { block(attempt) }
            } catch (e: RateLimitException) {
                if (attempt >= maxAttempts) throw e
                // Honor server-provided Retry-After when present, otherwise exponential backoff.
                val serverDelayMs = (e.retryAfterSeconds ?: 0).toLong() * 1000L
                val backoffMs = max(baseDelayMs * (1L shl (attempt - 1)), serverDelayMs)
                val cappedMs = min(backoffMs, 60_000L)
                _rateLimitState.update {
                    it.copy(
                        isCoolingDown = true,
                        remainingSeconds = (cappedMs / 1000L).toInt(),
                        currentExecutingTask = "$taskName (限流退避重试 ${attempt + 1}/$maxAttempts, 等待 ${cappedMs / 1000L}s...)"
                    )
                }
                delay(cappedMs)
                attempt++
            }
        }
    }
}

/**
 * Signals a transient, retryable server-side rate limit. Carries the optional `Retry-After`
 * delay (in seconds) parsed from the response so callers can back off precisely.
 */
class RateLimitException(
    message: String,
    val retryAfterSeconds: Int? = null
) : Exception(message)
