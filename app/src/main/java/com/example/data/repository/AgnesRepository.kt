package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.AgnesClient
import com.example.data.api.RateLimitManager
import com.example.data.local.AppDatabase
import com.example.data.model.AgnesApiConfig
import com.example.data.model.ChatMessage
import com.example.data.model.GenerationProject
import com.example.data.model.GenerationStatus
import com.example.data.model.ProjectType
import com.example.data.model.RateLimitState
import com.example.data.model.SceneClip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AgnesRepository(
    private val context: Context,
    private val database: AppDatabase,
    val rateLimitManager: RateLimitManager,
    val agnesClient: AgnesClient
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("agnes_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<AgnesApiConfig> = _configFlow.asStateFlow()

    // Observe projects & clips
    val allProjects: Flow<List<GenerationProject>> = database.projectDao().getAllProjects()
    val chatMessages: Flow<List<ChatMessage>> = database.chatMessageDao().getAllMessages()
    val rateLimitState: StateFlow<RateLimitState> = rateLimitManager.rateLimitState

    fun getClipsForProject(projectId: String): Flow<List<SceneClip>> {
        return database.sceneClipDao().getClipsForProject(projectId)
    }

    fun getProject(projectId: String): Flow<GenerationProject?> {
        return database.projectDao().getProjectById(projectId)
    }

    private fun loadConfig(): AgnesApiConfig {
        val rawEndpoint = prefs.getString("endpoint_url", "https://api.agnes-ai.cn/v1") ?: "https://api.agnes-ai.cn/v1"
        val endpoint = if (rawEndpoint.contains("api.agnes.ai") || rawEndpoint.isBlank()) {
            "https://api.agnes-ai.cn/v1"
        } else {
            rawEndpoint
        }

        val rawChat = prefs.getString("chat_model_name", "gpt-4o") ?: "gpt-4o"
        val chatModel = if (rawChat == "agnes-chat-pro") "gpt-4o" else rawChat

        val rawImage = prefs.getString("model_name", "flux-1-dev") ?: "flux-1-dev"
        val imageModel = if (rawImage == "agnes-vision-ultra") "flux-1-dev" else rawImage

        val rawVideo = prefs.getString("video_model_name", "kling-v1") ?: "kling-v1"
        val videoModel = if (rawVideo == "agnes-video-gen-v2") "kling-v1" else rawVideo

        return AgnesApiConfig(
            apiKey = prefs.getString("api_key", "") ?: "",
            endpointUrl = endpoint,
            chatModelName = chatModel,
            modelName = imageModel,
            videoModelName = videoModel,
            rateLimitSeconds = prefs.getInt("rate_limit_seconds", 60),
            autoStitchVideos = prefs.getBoolean("auto_stitch", true),
            customAuthHeader = prefs.getString("auth_header", "Bearer") ?: "Bearer"
        )
    }

    fun saveConfig(config: AgnesApiConfig) {
        prefs.edit()
            .putString("api_key", config.apiKey.trim())
            .putString("endpoint_url", config.endpointUrl.trim())
            .putString("chat_model_name", config.chatModelName.trim())
            .putString("model_name", config.modelName.trim())
            .putString("video_model_name", config.videoModelName.trim())
            .putInt("rate_limit_seconds", config.rateLimitSeconds)
            .putBoolean("auto_stitch", config.autoStitchVideos)
            .putString("auth_header", config.customAuthHeader.trim())
            .apply()

        rateLimitManager.updateCooldownInterval(config.rateLimitSeconds)
        _configFlow.value = config
    }

    suspend fun fetchRemoteModels(): Result<List<String>> {
        return agnesClient.fetchAvailableModels(_configFlow.value)
    }

    suspend fun generateChatReply(history: List<ChatMessage>, prompt: String): Result<String> {
        return agnesClient.generateChatReply(_configFlow.value, history, prompt)
    }

    suspend fun testConnection(): Result<String> {
        return agnesClient.testConnection(_configFlow.value)
    }

    /**
     * Start Image-to-Image Generation
     */
    suspend fun generateImageToImage(
        prompt: String,
        stylePreset: String,
        aspectRatio: String,
        sourceImageUri: String?
    ): Result<GenerationProject> {
        val project = GenerationProject(
            id = UUID.randomUUID().toString(),
            title = if (prompt.isNotBlank()) prompt.take(30) else "智能图片变奏重绘",
            type = ProjectType.IMAGE_TO_IMAGE,
            prompt = prompt,
            sourceImageUri = sourceImageUri,
            stylePreset = stylePreset,
            aspectRatio = aspectRatio,
            status = GenerationStatus.WAITING_RATE_LIMIT,
            statusMessage = "排队等待 Dream AI API 调度中..."
        )
        database.projectDao().insertProject(project)

        val result = agnesClient.generateImageToImage(
            config = _configFlow.value,
            prompt = prompt,
            stylePreset = stylePreset,
            aspectRatio = aspectRatio,
            sourceImageUri = sourceImageUri
        )

        return if (result.isSuccess) {
            val updated = project.copy(
                resultImageUri = result.getOrNull(),
                status = GenerationStatus.COMPLETED,
                statusMessage = "生成成功！"
            )
            database.projectDao().updateProject(updated)
            Result.success(updated)
        } else {
            val failed = project.copy(
                status = GenerationStatus.FAILED,
                statusMessage = "生成失败: ${result.exceptionOrNull()?.message}",
                error = result.exceptionOrNull()?.message
            )
            database.projectDao().updateProject(failed)
            Result.failure(result.exceptionOrNull() ?: Exception("生成失败"))
        }
    }

    /**
     * Complete Pipeline:
     * 1. Plan Video Script (Scene Clips)
     * 2. Sequential Rate-Limited Generation for each clip (1 per 60s)
     * 3. Stitch into Master Video
     */
    suspend fun startFullVideoPipeline(
        themePrompt: String,
        sourceImageUri: String?,
        sceneCount: Int = 4,
        stylePreset: String = "Cinematic 3D",
        onProgress: (String) -> Unit = {}
    ): Result<GenerationProject> {
        val projectId = UUID.randomUUID().toString()
        val project = GenerationProject(
            id = projectId,
            title = if (themePrompt.isNotBlank()) themePrompt.take(30) else "多段分镜拼接视频",
            type = ProjectType.VIDEO_SCRIPT_AND_STITCH,
            prompt = themePrompt,
            sourceImageUri = sourceImageUri,
            totalClips = sceneCount,
            completedClips = 0,
            stylePreset = stylePreset,
            status = GenerationStatus.SCRIPTING,
            statusMessage = "Dream AI 正在规划 $sceneCount 段电影分镜脚本..."
        )
        database.projectDao().insertProject(project)

        // Step 1: Generate Script
        onProgress("正在通过 Dream AI 构思分镜脚本...")
        val scriptResult = agnesClient.generateVideoScript(
            config = _configFlow.value,
            themePrompt = themePrompt,
            sceneCount = sceneCount,
            stylePreset = stylePreset
        )

        if (scriptResult.isFailure) {
            val errProject = project.copy(
                status = GenerationStatus.FAILED,
                statusMessage = "分镜规划失败: ${scriptResult.exceptionOrNull()?.message}",
                error = scriptResult.exceptionOrNull()?.message
            )
            database.projectDao().updateProject(errProject)
            return Result.failure(scriptResult.exceptionOrNull() ?: Exception("脚本生成失败"))
        }

        val scenes = scriptResult.getOrThrow().map { it.copy(projectId = projectId) }
        database.sceneClipDao().insertClips(scenes)

        val updatedProject = project.copy(
            totalClips = scenes.size,
            status = GenerationStatus.GENERATING_CLIPS,
            statusMessage = "已生成 ${scenes.size} 个分镜脚本，准备依次排队生成多段视频 (限速 1次/分)..."
        )
        database.projectDao().updateProject(updatedProject)

        // Step 2: Sequential Video Generation for each clip respecting 1 request/min
        val completedClips = mutableListOf<SceneClip>()
        for ((index, clip) in scenes.withIndex()) {
            onProgress("正在生成分镜 ${clip.sceneNumber}/${scenes.size}: ${clip.sceneTitle} (每分钟生成1段)...")
            
            // Mark current clip as generating so carousel UI shows loading animation for this scene
            val generatingClip = clip.copy(
                status = GenerationStatus.GENERATING_CLIPS,
                videoUrl = null,
                previewThumbnailUrl = null
            )
            database.sceneClipDao().updateClip(generatingClip)

            val clipGenResult = agnesClient.generateSceneVideoClip(
                config = _configFlow.value,
                scene = clip,
                projectId = projectId,
                stylePreset = stylePreset,
                sourceImageUri = sourceImageUri
            )

            if (clipGenResult.isSuccess) {
                val clipPath = clipGenResult.getOrThrow()
                val updatedClip = clip.copy(
                    videoUrl = clipPath,
                    previewThumbnailUrl = clipPath,
                    status = GenerationStatus.COMPLETED
                )
                database.sceneClipDao().updateClip(updatedClip)
                completedClips.add(updatedClip)

                database.projectDao().updateProject(
                    updatedProject.copy(
                        completedClips = completedClips.size,
                        statusMessage = "已完成分镜 ${completedClips.size}/${scenes.size} 视频生成"
                    )
                )
            } else {
                val failedClip = clip.copy(
                    status = GenerationStatus.FAILED,
                    error = clipGenResult.exceptionOrNull()?.message
                )
                database.sceneClipDao().updateClip(failedClip)
            }
        }

        // Step 3: Stitching
        if (completedClips.isNotEmpty() && _configFlow.value.autoStitchVideos) {
            onProgress("正在将 ${completedClips.size} 段视频无缝拼接合成为完整长视频...")
            database.projectDao().updateProject(
                updatedProject.copy(
                    status = GenerationStatus.STITCHING,
                    statusMessage = "正在渲染拼接所有视频片段..."
                )
            )

            val stitchResult = agnesClient.stitchVideoClips(
                projectId = projectId,
                projectTitle = project.title,
                clips = completedClips
            )

            val finalVideoPath = stitchResult.getOrNull() ?: completedClips.first().videoUrl
            val finalDuration = completedClips.sumOf { it.durationSeconds }

            val finishedProject = updatedProject.copy(
                resultVideoUri = finalVideoPath,
                durationSeconds = finalDuration,
                status = GenerationStatus.COMPLETED,
                statusMessage = "全部 ${completedClips.size} 段分镜视频已成功生成并拼接！"
            )
            database.projectDao().updateProject(finishedProject)
            return Result.success(finishedProject)
        } else {
            val finishedProject = updatedProject.copy(
                status = GenerationStatus.COMPLETED,
                statusMessage = "视频片段生成完毕！"
            )
            database.projectDao().updateProject(finishedProject)
            return Result.success(finishedProject)
        }
    }

    suspend fun sendChatMessage(userText: String, attachedImageUri: String? = null): ChatMessage {
        val userMsg = ChatMessage(
            sender = "user",
            content = userText,
            attachedImageUri = attachedImageUri
        )
        database.chatMessageDao().insertMessage(userMsg)
        return userMsg
    }

    suspend fun saveAgentReply(replyText: String, relatedProjectId: String? = null, actionType: String? = null): ChatMessage {
        val agentMsg = ChatMessage(
            sender = "agnes_agent",
            content = replyText,
            relatedProjectId = relatedProjectId,
            actionType = actionType
        )
        database.chatMessageDao().insertMessage(agentMsg)
        return agentMsg
    }

    suspend fun deleteProject(project: GenerationProject) {
        database.sceneClipDao().deleteClipsForProject(project.id)
        database.projectDao().deleteProject(project)
    }

    suspend fun clearChatHistory() {
        database.chatMessageDao().clearAllMessages()
    }
}
