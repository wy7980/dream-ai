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
}
