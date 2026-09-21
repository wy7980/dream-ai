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
    val status: GenerationStatus = GenerationStatus.IDLE,
    val cooldownRemainingSeconds: Int = 0,
    val error: String? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val videoModelName: String = "kling-v1", // Video Generation Model
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

data class RateLimitState(
    val isCoolingDown: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalCooldownSeconds: Int = 60,
    val lastCallTime: Long = 0L,
    val pendingQueueCount: Int = 0,
    val currentExecutingTask: String? = null
)
