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
    val actionType: String? = null, // "IMAGE_RESULT", "VIDEO_SCRIPT", "QUEUE_STATUS"
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatIntentMode {
    AUTO,
    CHAT,
    IMAGE_GEN,
    VIDEO_GEN
}

data class AgnesApiConfig(
    val apiKey: String = "",
    val endpointUrl: String = "https://api.agnes-ai.cn/v1",
    val chatModelName: String = "gpt-4o",
    val modelName: String = "flux-1-dev", // Image Generation / Remix Model
    val videoModelName: String = "kling-v1", // Video Generation Model
    val rateLimitSeconds: Int = 60, // Strictly 1 request per minute (60s) for Image & Video Generation
    val autoStitchVideos: Boolean = true,
    val customAuthHeader: String = "Bearer"
)

data class RateLimitState(
    val isCoolingDown: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalCooldownSeconds: Int = 60,
    val lastCallTime: Long = 0L,
    val pendingQueueCount: Int = 0,
    val currentExecutingTask: String? = null
)
