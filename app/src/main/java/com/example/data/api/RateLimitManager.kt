package com.example.data.api

import com.example.data.model.RateLimitLane
import com.example.data.model.RateLimitLaneState
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

/**
 * Multi-lane rate limiter.
 *
 * The Agnes backend enforces its rate limit **per endpoint**, verified live on 2026-09-23:
 *  - `POST /v1/images/generations` (images) has its own bucket (≥12 concurrent requests succeeded);
 *  - `POST /v1/videos` (video task creation) has its own bucket (a 2nd concurrent create → 429);
 *  - `POST /v1/chat/completions` (script planning) has its own bucket.
 *
 * So each [RateLimitLane] gets an INDEPENDENT mutex + cooldown clock. Rendering a storyboard no
 * longer waits behind every preview image, and image generation no longer waits behind a video
 * create — they proceed in parallel, each respecting only its own quota.
 *
 * Note: a video task's long polling loop is NOT rate-limited at all (read-only); only the create
 * request spends a quota unit. Callers keep polling outside this limiter by design.
 */
class RateLimitManager(
    private var cooldownIntervalSeconds: Int = 60,
    lanes: List<RateLimitLane> = RateLimitLane.entries.toList()
) {
    private class Lane(
        val lane: RateLimitLane,
        var cooldownSeconds: Int,
        var lastRequestTimestamp: Long = 0L
    ) {
        val mutex = Mutex()
        val state = MutableStateFlow(RateLimitLaneState(lane = lane, totalCooldownSeconds = cooldownSeconds))
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Lane states keyed by enum. Insertion order == [RateLimitLane] declaration order. */
    private val laneMap: Map<RateLimitLane, Lane> = LinkedHashMap<RateLimitLane, Lane>().apply {
        lanes.forEach { put(it, Lane(lane = it, cooldownSeconds = cooldownIntervalSeconds)) }
    }

    private val _rateLimitState = MutableStateFlow(composeState())
    /** Composite snapshot across all lanes (scalars mirror the most active lane). */
    val rateLimitState: StateFlow<RateLimitState> = _rateLimitState.asStateFlow()

    init {
        // Background ticker for live UI countdowns across every lane.
        scope.launch {
            while (true) {
                delay(1000L)
                tickAll()
            }
        }
    }

    private fun laneOf(lane: RateLimitLane): Lane =
        laneMap[lane] ?: error("RateLimitManager was not initialised with lane $lane")

    private fun tickAll() {
        val now = System.currentTimeMillis()
        laneMap.values.forEach { l ->
            val elapsed = (now - l.lastRequestTimestamp) / 1000L
            val remaining = max(0L, l.cooldownSeconds - elapsed).toInt()
            l.state.update { cur ->
                cur.copy(
                    isCoolingDown = remaining > 0 && l.lastRequestTimestamp > 0,
                    remainingSeconds = remaining
                )
            }
        }
        _rateLimitState.value = composeState()
    }

    /** Fold the per-lane states into one composite snapshot for the existing single-state UI. */
    private fun composeState(): RateLimitState {
        val laneStates = laneMap.values.map { it.state.value }
        val headline = laneStates.maxByOrNull { it.priorityScore }
        return RateLimitState(
            isCoolingDown = laneStates.any { it.isCoolingDown },
            remainingSeconds = laneStates.maxOfOrNull { it.remainingSeconds } ?: 0,
            totalCooldownSeconds = headline?.totalCooldownSeconds ?: cooldownIntervalSeconds,
            lastCallTime = laneStates.maxOfOrNull { it.lastCallTime } ?: 0L,
            pendingQueueCount = laneStates.sumOf { it.pendingQueueCount },
            currentExecutingTask = headline?.currentExecutingTask,
            lanes = laneStates
        )
    }

    /** Apply a new cooldown to every lane (kept for the single-value settings field). */
    fun updateCooldownInterval(seconds: Int) {
        cooldownIntervalSeconds = seconds
        laneMap.values.forEach { l ->
            l.cooldownSeconds = seconds
            l.state.update { it.copy(totalCooldownSeconds = seconds) }
        }
        _rateLimitState.value = composeState()
    }

    suspend fun <T> executeRateLimited(
        lane: RateLimitLane,
        taskName: String,
        block: suspend () -> T
    ): T {
        val l = laneOf(lane)
        l.state.update { it.copy(pendingQueueCount = it.pendingQueueCount + 1) }
        _rateLimitState.value = composeState()

        return try {
            l.mutex.withLock {
                l.state.update {
                    it.copy(
                        currentExecutingTask = taskName,
                        pendingQueueCount = max(0, it.pendingQueueCount - 1)
                    )
                }
                _rateLimitState.value = composeState()

                // Check this lane's own remaining cooldown.
                val now = System.currentTimeMillis()
                val elapsedSeconds = (now - l.lastRequestTimestamp) / 1000L
                val waitSeconds = (l.cooldownSeconds - elapsedSeconds).toInt()

                if (waitSeconds > 0 && l.lastRequestTimestamp > 0) {
                    var remaining = waitSeconds
                    while (remaining > 0) {
                        l.state.update {
                            it.copy(
                                isCoolingDown = true,
                                remainingSeconds = remaining,
                                currentExecutingTask = "$taskName (限速冷却等待中 $remaining 秒...)"
                            )
                        }
                        _rateLimitState.value = composeState()
                        delay(1000L)
                        remaining--
                    }
                }

                l.state.update {
                    it.copy(
                        isCoolingDown = false,
                        remainingSeconds = 0,
                        currentExecutingTask = "$taskName (正在调用 Agnes API...)"
                    )
                }
                _rateLimitState.value = composeState()

                // Mark this lane's timestamp right before the API call.
                l.lastRequestTimestamp = System.currentTimeMillis()
                l.state.update { it.copy(lastCallTime = l.lastRequestTimestamp) }

                block()
            }
        } finally {
            l.state.update { it.copy(currentExecutingTask = null) }
            _rateLimitState.value = composeState()
        }
    }

    /** Remaining cooldown for one lane (defaults to the max across lanes when omitted). */
    fun getRemainingSeconds(lane: RateLimitLane? = null): Int {
        if (lane == null) return _rateLimitState.value.remainingSeconds
        val l = laneOf(lane)
        val elapsed = (System.currentTimeMillis() - l.lastRequestTimestamp) / 1000L
        return max(0L, l.cooldownSeconds - elapsed).toInt()
    }

    /**
     * Runs [block] inside one lane's limiter, retrying with exponential backoff when the server
     * signals a transient rate limit (HTTP 429 / `rate_limit_exceeded`). [block] must throw
     * [RateLimitException] for a retryable response and any other exception to fail fast.
     *
     * Backoff sleeps do NOT hold the lane mutex, so other tasks in the same lane stay queued behind
     * the retry (correct: they'd hit the same server quota) while other lanes run undisturbed.
     */
    suspend fun <T> executeRateLimitedWithRetry(
        lane: RateLimitLane,
        taskName: String,
        maxAttempts: Int = 6,
        baseDelayMs: Long = 5_000L,
        block: suspend (attempt: Int) -> T
    ): T {
        val l = laneOf(lane)
        var attempt = 1
        while (true) {
            try {
                return executeRateLimited(lane, taskName) { block(attempt) }
            } catch (e: RateLimitException) {
                if (attempt >= maxAttempts) throw e
                // Honor server-provided Retry-After when present, otherwise exponential backoff.
                val serverDelayMs = (e.retryAfterSeconds ?: 0).toLong() * 1000L
                val backoffMs = max(baseDelayMs * (1L shl (attempt - 1)), serverDelayMs)
                val cappedMs = min(backoffMs, 60_000L)
                l.state.update {
                    it.copy(
                        isCoolingDown = true,
                        remainingSeconds = (cappedMs / 1000L).toInt(),
                        currentExecutingTask = "$taskName (限流退避重试 ${attempt + 1}/$maxAttempts, 等待 ${cappedMs / 1000L}s...)"
                    )
                }
                _rateLimitState.value = composeState()
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
