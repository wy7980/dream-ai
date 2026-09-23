package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ProjectType {
    IMAGE_TO_IMAGE,
    VIDEO_SCRIPT_AND_STITCH
}

enum class GenerationStatus {
    IDLE,
    WAITING_RATE_LIMIT,
    SCRIPTING,
    /**
     * Script planned, waiting for the user to review / edit the storyboard (scene count, prompts,
     * duration) before any rate-limited video request is spent. This is the two-phase hand-off:
     * phase 1 stops here, phase 2 ([GENERATING_CLIPS]) starts only on explicit confirmation.
     */
    AWAITING_REVIEW,
    GENERATING_CLIPS,
    STITCHING,
    COMPLETED,
    FAILED
}

@Entity(tableName = "projects")
data class GenerationProject(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: ProjectType,
    val prompt: String,
    /** Owning conversation, so a project (and its long-running video task) can be traced back
     *  to the chat session that started it. Nullable for rows created before sessions existed. */
    val sessionId: String? = null,
    val sourceImageUri: String? = null,
    val sourceImageBase64: String? = null,
    val resultImageUri: String? = null,
    val resultVideoUri: String? = null,
    val totalClips: Int = 0,
    val completedClips: Int = 0,
    val status: GenerationStatus = GenerationStatus.IDLE,
    val statusMessage: String = "",
    val stylePreset: String = "Cinematic 3D",
    val aspectRatio: String = "16:9",
    // Global "style bible" produced by the script planner: locks protagonist / wardrobe /
    // environment / lighting / color grading so every scene of one project stays consistent.
    val styleBible: String? = null,
    // IP-Adapter-style style anchor: one key-art image (protagonist + environment rendered in the
    // locked visualStyle). Generated once per project, shown to the user in the review panel and
    // regeneratable. When present, every scene is rendered in Agnes 2.5 `reference` mode with this
    // image as <Picture 1>, so the rendering medium cannot drift shot-to-shot (watercolour → anime
    // → live action). Null = fall back to text/keyframe style locking only.
    val styleReferenceImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val error: String? = null
)

@Entity(tableName = "scene_clips")
data class SceneClip(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val sceneNumber: Int,
    val sceneTitle: String,
    val visualPrompt: String,
    val cameraMovement: String, // e.g. "Slow Dolly In", "Drone Orbit", "Pan Left to Right"
    val narration: String,
    val durationSeconds: Int = 10,
    val videoUrl: String? = null,
    val previewThumbnailUrl: String? = null,
    val taskId: String? = null, // Service provider returned task_id / video_id
    val statusMessage: String? = null, // Detailed status for debugging
    val status: GenerationStatus = GenerationStatus.IDLE,
    val cooldownRemainingSeconds: Int = 0,
    val error: String? = null,
    /**
     * True when this scene was planned but has never been rendered yet (the project is still in
     * the review phase). Kept separate from [status] so a planned scene is not mistaken for an
     * IDLE/unplanned row, and so "只生成本幕" can target exactly the un-rendered scenes.
     */
    val isDraft: Boolean = false
)

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    // Owning conversation. Nullable for rows created before sessions existed.
    val sessionId: String? = null,
    val sender: String, // "user", "agnes_agent", "system"
    val content: String,
    val attachedImageUri: String? = null,
    val relatedProjectId: String? = null,
    val actionType: String? = null, // "IMAGE_RESULT", "VIDEO_SCRIPT", "DOCUMENT_RESULT", "QUEUE_STATUS"
    val documentUri: String? = null,
    val documentType: String? = null, // "WORD", "PDF", "EXCEL"
    val documentName: String? = null,
    val documentSize: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatIntentMode {
    AUTO,
    CHAT,
    IMAGE_GEN,
    VIDEO_GEN
}

data class AIProvider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endpointUrl: String,
    val apiKey: String = "",
    val authHeader: String = "Bearer",
    val customModels: List<String> = emptyList(),
    val isDefault: Boolean = false,
    val description: String = ""
)

data class AgnesApiConfig(
    val apiKey: String = "",
    val tavilyApiKey: String = "",
    val endpointUrl: String = "https://api.agnes-ai.cn/v1",
    val chatModelName: String = "gpt-4o",
    val modelName: String = "flux-1-dev", // Image Generation / Remix Model
    val videoModelName: String = "agnes-video-v2.0", // Video Generation Model: "agnes-video-v2.0" or "agnes-video-2.5-flash"
    val chatProviderId: String = "agnes-default",
    val imageProviderId: String = "agnes-default",
    val videoProviderId: String = "agnes-default",
    val providers: List<AIProvider> = listOf(
        AIProvider(
            id = "agnes-default",
            name = "Dream AI (官方代理)",
            endpointUrl = "https://api.agnes-ai.cn/v1",
            apiKey = "",
            authHeader = "Bearer",
            isDefault = true,
            description = "官方高可用多模态聚合通道，支持主流对话、生图、生视频模型"
        ),
        AIProvider(
            id = "openai-official",
            name = "OpenAI 官方",
            endpointUrl = "https://api.openai.com/v1",
            apiKey = "",
            authHeader = "Bearer",
            isDefault = false,
            description = "OpenAI 官方原生接口 (GPT-4o, DALL·E 3, Sora)"
        ),
        AIProvider(
            id = "siliconflow",
            name = "SiliconFlow 硅基流动",
            endpointUrl = "https://api.siliconflow.cn/v1",
            apiKey = "",
            authHeader = "Bearer",
            isDefault = false,
            description = "国内高速低延迟云端算力 (DeepSeek, Flux, Qwen)"
        ),
        AIProvider(
            id = "deepseek-official",
            name = "DeepSeek 官方",
            endpointUrl = "https://api.deepseek.com/v1",
            apiKey = "",
            authHeader = "Bearer",
            isDefault = false,
            description = "DeepSeek 官方直连 API (deepseek-chat, deepseek-reasoner)"
        )
    ),
    val rateLimitSeconds: Int = 60, // Strictly 1 request per minute (60s) for Image & Video Generation
    val autoStitchVideos: Boolean = true,
    val isDarkTheme: Boolean = true,
    val customAuthHeader: String = "Bearer"
)

data class TavilySearchResultItem(
    val title: String,
    val url: String,
    val content: String,
    val score: Double = 0.0,
    val publishedDate: String? = null
)

data class TavilySearchResponse(
    val query: String,
    val answer: String? = null,
    val results: List<TavilySearchResultItem> = emptyList(),
    val rawJson: String? = null
)

/**
 * Fine-grained lifecycle stage of a long-running (rate-limited) generation task.
 *
 * [GenerationProject.status] is a coarse, UI-facing summary; this enum is the durable state
 * machine that lets an interrupted task be resumed after the app process dies (option B:
 * detect-and-resume on next launch). Only non-terminal stages are considered resumable.
 */
enum class TaskStage {
    /** Phase 1: the storyboard script is being planned (cheap, no video quota spent). */
    PLANNING,

    /** Phase 2: scenes are being rendered one by one; the remote task id is in [GenerationTask.remoteTaskId]. */
    RENDERING,

    /** All scenes rendered, the master video is being stitched. */
    STITCHING,

    /** Terminal: the whole pipeline finished successfully. */
    COMPLETED,

    /** Terminal: the pipeline stopped with an error; the user may retry. */
    FAILED
}

/**
 * One durable row per long-running video pipeline run.
 *
 * This is the "state machine on disk" that survives process death: every remote task id, the
 * current scene, the poll attempt counter and a heartbeat are persisted here so that on the next
 * app launch the pipeline can be detected and resumed (or honestly marked failed) instead of being
 * silently orphaned mid-flight.
 */
@Entity(tableName = "generation_tasks")
data class GenerationTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val sessionId: String? = null,
    val stage: TaskStage = TaskStage.PLANNING,
    /** Remote provider task/video id of the scene currently being polled (RENDERING only). */
    val remoteTaskId: String? = null,
    /** Local scene id currently being rendered, so a resume targets exactly the right row. */
    val currentClipId: String? = null,
    /** 1-based index of the scene currently being rendered, for a readable resume banner. */
    val currentSceneNumber: Int = 0,
    val totalScenes: Int = 0,
    /** Number of poll ticks already spent on [remoteTaskId] (survives restarts). */
    val pollCount: Int = 0,
    /** Last poll timestamp / last progress write, used as a liveness heartbeat. */
    val lastPolledAt: Long = 0L,
    val lastMessage: String = "",
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class RateLimitState(
    val isCoolingDown: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalCooldownSeconds: Int = 60,
    val lastCallTime: Long = 0L,
    val pendingQueueCount: Int = 0,
    val currentExecutingTask: String? = null
)

/**
 * Shared bounds for the storyboard scene count. Each scene costs one rate-limited video
 * request, so the range is intentionally bounded and enforced at every layer (UI, view
 * model, repository and API client).
 */
object VideoSceneLimits {
    const val MIN = 1
    const val MAX = 20
    const val DEFAULT = 1

    /**
     * Sentinel meaning "let the director model decide the scene count". Kept distinct from any
     * real count so callers can tell "auto" apart from "the model happened to pick 1".
     */
    const val AUTO = 0

    /**
     * Clamp an arbitrary caller-supplied scene count into the supported range. `AUTO` (0) passes
     * through untouched so the auto-planning path survives the defensive clamp.
     */
    fun clamp(count: Int): Int = if (count == AUTO) AUTO else count.coerceIn(MIN, MAX)
}

/**
 * Per-scene video duration bounds. The Agnes 2.5 video models accept `seconds`
 * from "4" to "12"; keeping the UI and every caller inside this window avoids
 * silent server-side clamping (and the confusing "I asked for 3s but got 4s" case).
 */
object VideoDurationLimits {
    const val MIN = 4
    const val MAX = 12
    const val DEFAULT = 5

    /** Sentinel meaning "let the director model decide the per-scene duration". */
    const val AUTO = 0

    /**
     * Clamp an arbitrary caller-supplied duration into the supported range. `AUTO` (0) passes
     * through untouched so the auto-planning path survives the defensive clamp.
     */
    fun clamp(seconds: Int): Int = if (seconds == AUTO) AUTO else seconds.coerceIn(MIN, MAX)
}
