package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.api.AgnesClient
import com.example.data.api.RateLimitManager
import com.example.data.local.AppDatabase
import com.example.data.model.AIProvider
import com.example.data.model.AgnesApiConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.GenerationProject
import com.example.data.model.GenerationStatus
import com.example.data.model.ProjectType
import com.example.data.model.RateLimitState
import com.example.data.model.SceneClip
import com.example.data.model.VideoDurationLimits
import com.example.data.model.VideoSceneLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AgnesRepository(
    private val context: Context,
    private val database: AppDatabase,
    val rateLimitManager: RateLimitManager,
    val agnesClient: AgnesClient
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("agnes_prefs", Context.MODE_PRIVATE)

    companion object {
        /** Placeholder title for a conversation that has not received its first user turn. */
        const val DEFAULT_SESSION_TITLE = "新对话"

        /** Lowest selectable storyboard scene count. */
        const val MIN_SCENE_COUNT = 1

        /** Highest selectable storyboard scene count (each scene costs one rate-limited request). */
        const val MAX_SCENE_COUNT = 20

        /** Curated fallback storyboard beats used when the planner returns too few scenes. */
        private val FALLBACK_SCENE_TEMPLATES = listOf(
            Triple(
                "启幕：宏大世界观展现",
                "航拍缓慢推远俯瞰未来都市全景，霓虹天际线与体积光渲染，晨曦穿透云层",
                "缓慢推远俯瞰，展现宏伟世界全貌与晨曦光影"
            ),
            Triple(
                "聚焦：关键主体与动态张力",
                "低角度跟拍镜头，主角发现脉动的量子晶体异常，能量微光映照面部",
                "低角度跟镜头推进，捕捉主体神秘能量脉动"
            ),
            Triple(
                "递进：环境探索与线索浮现",
                "手持视差推进穿过雨夜霓虹窄巷，全息线索逐一亮起，湿地倒影反射光斑",
                "手持视差推进，霓虹雨巷中全息线索逐一亮起"
            ),
            Triple(
                "高潮：能量爆发与视觉冲击",
                "快速推近并360度环绕拍摄能量爆发，发光粒子瀑布扩散，空间扭曲",
                "全方位旋转环绕特写，能量波纹与光子粒子爆发扩散"
            ),
            Triple(
                "转折：危机与抉择时刻",
                "升格急推特写主角面部，警报红光闪烁，碎片缓缓掠过，紧张氛围",
                "升格急推特写，警报闪烁、碎片掠过，危机与抉择降临"
            ),
            Triple(
                "尾声：电影级史诗定格",
                "电影感日落摇臂镜头缓缓升起，星空与暮色交融，霓虹地平线归于平静",
                "摇臂镜头升起，星空与余晖交织，定格电影级史诗终章"
            ),
            Triple(
                "余韵：未来无限延展",
                "微距镜头焦点由霓虹露珠缓慢转移到其中折射的无垠宇宙",
                "微距焦点转移，水滴中折射无垠宇宙光芒"
            )
        )
    }

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<AgnesApiConfig> = _configFlow.asStateFlow()

    // Observe projects & clips
    val allProjects: Flow<List<GenerationProject>> = database.projectDao().getAllProjects()
    val chatSessions: Flow<List<ChatSession>> = database.chatSessionDao().getAllSessions()

    /** Messages belonging to one conversation, oldest first. */
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> =
        database.chatMessageDao().getMessagesForSession(sessionId)
    val rateLimitState: StateFlow<RateLimitState> = rateLimitManager.rateLimitState

    /**
     * Persist user edits to a storyboard scene's creative fields (title / visual prompt /
     * camera movement / narration). Only the editable columns are touched so that a
     * generation pass writing status columns concurrently is never overwritten.
     */
    suspend fun updateClipPrompt(
        clipId: String,
        title: String,
        visualPrompt: String,
        cameraMovement: String,
        narration: String
    ): Result<Unit> = runCatching {
        database.sceneClipDao().updateClipPrompt(
            clipId = clipId,
            title = title.trim(),
            visualPrompt = visualPrompt.trim(),
            cameraMovement = cameraMovement.trim(),
            narration = narration.trim()
        )
    }

    fun getClipsForProject(projectId: String): Flow<List<SceneClip>> {
        return database.sceneClipDao().getClipsForProject(projectId)
    }

    fun getProject(projectId: String): Flow<GenerationProject?> {
        return database.projectDao().getProjectById(projectId)
    }

    suspend fun getProjectDirect(projectId: String): GenerationProject? {
        return database.projectDao().getProjectDirect(projectId)
    }

    private fun loadConfig(): AgnesApiConfig {
        val defaultConfig = AgnesApiConfig()
        val rawEndpoint = prefs.getString("endpoint_url", defaultConfig.endpointUrl) ?: defaultConfig.endpointUrl
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

        val providersJsonStr = prefs.getString("providers_json", null)
        val providersList = if (!providersJsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(providersJsonStr)
                val list = mutableListOf<AIProvider>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val customModelsArr = obj.optJSONArray("customModels")
                    val customModelsList = mutableListOf<String>()
                    if (customModelsArr != null) {
                        for (j in 0 until customModelsArr.length()) {
                            customModelsList.add(customModelsArr.getString(j))
                        }
                    }
                    list.add(
                        AIProvider(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Custom Provider"),
                            endpointUrl = obj.optString("endpointUrl", "https://api.agnes-ai.cn/v1"),
                            apiKey = obj.optString("apiKey", ""),
                            authHeader = obj.optString("authHeader", "Bearer"),
                            customModels = customModelsList,
                            isDefault = obj.optBoolean("isDefault", false),
                            description = obj.optString("description", "")
                        )
                    )
                }
                if (list.isNotEmpty()) list else defaultConfig.providers
            } catch (e: Exception) {
                defaultConfig.providers
            }
        } else {
            defaultConfig.providers
        }

        val chatProvId = prefs.getString("chat_provider_id", "agnes-default") ?: "agnes-default"
        val imgProvId = prefs.getString("image_provider_id", "agnes-default") ?: "agnes-default"
        val vidProvId = prefs.getString("video_provider_id", "agnes-default") ?: "agnes-default"

        return AgnesApiConfig(
            apiKey = prefs.getString("api_key", "") ?: "",
            tavilyApiKey = prefs.getString("tavily_api_key", "") ?: "",
            endpointUrl = endpoint,
            chatModelName = chatModel,
            modelName = imageModel,
            videoModelName = videoModel,
            chatProviderId = chatProvId,
            imageProviderId = imgProvId,
            videoProviderId = vidProvId,
            providers = providersList,
            rateLimitSeconds = prefs.getInt("rate_limit_seconds", 60),
            autoStitchVideos = prefs.getBoolean("auto_stitch", true),
            isDarkTheme = prefs.getBoolean("is_dark_theme", true),
            customAuthHeader = prefs.getString("auth_header", "Bearer") ?: "Bearer"
        )
    }

    fun saveConfig(config: AgnesApiConfig) {
        val providersArray = JSONArray()
        config.providers.forEach { provider ->
            val obj = JSONObject().apply {
                put("id", provider.id)
                put("name", provider.name)
                put("endpointUrl", provider.endpointUrl)
                put("apiKey", provider.apiKey)
                put("authHeader", provider.authHeader)
                put("isDefault", provider.isDefault)
                put("description", provider.description)
                val modelsArr = JSONArray()
                provider.customModels.forEach { modelsArr.put(it) }
                put("customModels", modelsArr)
            }
            providersArray.put(obj)
        }

        prefs.edit()
            .putString("api_key", config.apiKey.trim())
            .putString("tavily_api_key", config.tavilyApiKey.trim())
            .putString("endpoint_url", config.endpointUrl.trim())
            .putString("chat_model_name", config.chatModelName.trim())
            .putString("model_name", config.modelName.trim())
            .putString("video_model_name", config.videoModelName.trim())
            .putString("chat_provider_id", config.chatProviderId)
            .putString("image_provider_id", config.imageProviderId)
            .putString("video_provider_id", config.videoProviderId)
            .putString("providers_json", providersArray.toString())
            .putInt("rate_limit_seconds", config.rateLimitSeconds)
            .putBoolean("auto_stitch", config.autoStitchVideos)
            .putBoolean("is_dark_theme", config.isDarkTheme)
            .putString("auth_header", config.customAuthHeader.trim())
            .apply()

        rateLimitManager.updateCooldownInterval(config.rateLimitSeconds)
        _configFlow.value = config
    }

    suspend fun fetchRemoteModels(): Result<List<String>> {
        return agnesClient.fetchAvailableModels(_configFlow.value)
    }

    suspend fun fetchModelsForProvider(provider: AIProvider): Result<List<String>> {
        return agnesClient.fetchAvailableModelsForProvider(provider)
    }

    suspend fun testProviderConnection(provider: AIProvider): Result<String> {
        return agnesClient.testProviderConnection(provider)
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
        videoModel: String? = null,
        aspectRatio: String = "16:9",
        durationPerScene: Int = VideoDurationLimits.DEFAULT,
        onProgress: (String) -> Unit = {}
    ): Result<GenerationProject> {
        val effectiveModel = videoModel?.trim()?.ifBlank { null } ?: _configFlow.value.videoModelName
        // Two modes, both defended against bad callers:
        // - AUTO: the director model decides the scene count and per-scene duration from the material.
        // - Pinned: the caller's numbers are obeyed, clamped into the contract range.
        val autoSceneCount = sceneCount == VideoSceneLimits.AUTO
        val autoDuration = durationPerScene == VideoDurationLimits.AUTO
        val requestedSceneCount = if (autoSceneCount) {
            VideoSceneLimits.DEFAULT // placeholder; replaced once the script is planned
        } else {
            sceneCount.coerceIn(MIN_SCENE_COUNT, MAX_SCENE_COUNT)
        }
        // Per-scene duration is user-selectable from 4 to 12s; clamp so a bad caller can never
        // push an out-of-contract `seconds` value to the video API.
        val safeDurationPerScene = if (autoDuration) VideoDurationLimits.DEFAULT else VideoDurationLimits.clamp(durationPerScene)
        val projectId = UUID.randomUUID().toString()
        val project = GenerationProject(
            id = projectId,
            title = if (themePrompt.isNotBlank()) themePrompt.take(30) else "多段分镜拼接视频",
            type = ProjectType.VIDEO_SCRIPT_AND_STITCH,
            prompt = themePrompt,
            sourceImageUri = sourceImageUri,
            totalClips = requestedSceneCount,
            completedClips = 0,
            stylePreset = stylePreset,
            aspectRatio = aspectRatio,
            durationSeconds = safeDurationPerScene * requestedSceneCount,
            status = GenerationStatus.SCRIPTING,
            statusMessage = if (autoSceneCount) {
                "Dream AI 正在根据素材自动规划分镜数量与时长 (模型: $effectiveModel)..."
            } else {
                "Dream AI 正在规划 $requestedSceneCount 段电影分镜脚本 (模型: $effectiveModel)..."
            }
        )
        database.projectDao().insertProject(project)

        // Step 1: Generate Script
        onProgress("正在通过 Dream AI 构思分镜脚本 (目标模型: $effectiveModel, 比例: $aspectRatio)...")
        val scriptResult = agnesClient.generateVideoScript(
            config = _configFlow.value,
            themePrompt = themePrompt,
            sceneCount = if (autoSceneCount) VideoSceneLimits.AUTO else requestedSceneCount,
            stylePreset = stylePreset,
            durationPerScene = if (autoDuration) VideoDurationLimits.AUTO else safeDurationPerScene
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

        val script = scriptResult.getOrThrow()
        val styleBible = script.styleBible
        // In auto mode the model's own scene count wins (it counted the material's natural units);
        // in pinned mode we still normalise the model output to exactly the requested count.
        val targetSceneCount = if (autoSceneCount) {
            VideoSceneLimits.clamp(script.recommendedSceneCount.takeIf { it > 0 } ?: script.scenes.size)
        } else {
            requestedSceneCount
        }
        val targetDurationPerScene = if (autoDuration) {
            VideoDurationLimits.clamp(script.recommendedDurationPerScene ?: safeDurationPerScene)
        } else {
            safeDurationPerScene
        }
        val scenes = normalizeScenes(script.scenes, targetSceneCount, themePrompt, stylePreset)
            .map { it.copy(projectId = projectId, durationSeconds = targetDurationPerScene) }
        database.sceneClipDao().insertClips(scenes)

        // Deterministic per-project seed: keeps the render stable across re-runs of the same
        // project (less flicker / subject drift) while still varying between projects.
        val projectSeed = (projectId.hashCode().toLong() and 0x7FFFFFFFL)

        val updatedProject = project.copy(
            totalClips = scenes.size,
            durationSeconds = targetDurationPerScene * scenes.size,
            styleBible = styleBible,
            status = GenerationStatus.GENERATING_CLIPS,
            statusMessage = "已生成 ${scenes.size} 个分镜脚本（${targetDurationPerScene}秒/幕，成片约 ${targetDurationPerScene * scenes.size} 秒），准备依次排队生成多段视频 (限速 1次/分, 模型: $effectiveModel)..."
        )
        database.projectDao().updateProject(updatedProject)

        // Step 2: Sequential Video Generation for each clip respecting 1 request/min.
        // The previous scene's last frame is chained in as the next scene's first frame so the
        // clips flow continuously instead of being independent (jumpy) renders.
        val completedClips = mutableListOf<SceneClip>()
        var prevFrameDataUri: String? = null
        for ((index, clip) in scenes.withIndex()) {
            onProgress("正在生成分镜 ${clip.sceneNumber}/${scenes.size}: ${clip.sceneTitle} (模型: $effectiveModel, 1段/分)...")

            val chainedFromPrev = index > 0 && prevFrameDataUri != null
            if (chainedFromPrev) {
                onProgress("分镜 ${clip.sceneNumber}: 已提取上一分镜末帧，保持画面连续衔接...")
            }

            // Dual-frame control: predict this shot's END frame from its start frame + prompt, then
            // let the video model interpolate between the two. This is what removes the "jump" at
            // the seam — the clip is no longer free-running from a single anchor. If prediction
            // fails we degrade gracefully to single-frame keyframe control.
            var predictedEndFrameUri: String? = null
            if (chainedFromPrev) {
                onProgress("分镜 ${clip.sceneNumber}: 正在预测本段尾帧以锁定运镜轨迹...")
                val endFramePrompt = buildString {
                    append("Cinematic final frame of the shot described as: ")
                    append(clip.visualPrompt)
                    if (clip.cameraMovement.isNotBlank()) append(", camera movement: ${clip.cameraMovement}")
                    append(", style: $stylePreset")
                    if (!styleBible.isNullOrBlank()) append(", consistent with: $styleBible")
                    append(". This is the LAST frame, so the action has advanced to its end state while keeping the exact same subject, wardrobe, lighting and color grading as the first frame.")
                }
                val endFrameResult = agnesClient.generateFrameImage(
                    config = _configFlow.value,
                    prompt = endFramePrompt,
                    firstFrameDataUri = prevFrameDataUri,
                    aspectRatio = aspectRatio
                )
                predictedEndFrameUri = endFrameResult.getOrNull()
                if (predictedEndFrameUri == null) {
                    Log.w("AgnesRepository", "尾帧预测失败，降级为单帧首帧控制: ${endFrameResult.exceptionOrNull()?.message}")
                }
            }

            // Mark current clip as generating so carousel UI shows loading animation for this scene
            val generatingClip = clip.copy(
                status = GenerationStatus.GENERATING_CLIPS,
                videoUrl = null,
                previewThumbnailUrl = null,
                statusMessage = if (chainedFromPrev) "已用上一分镜末帧续接，生成中..." else null
            )
            database.sceneClipDao().updateClip(generatingClip)

            val clipGenResult = agnesClient.generateSceneVideoClip(
                config = _configFlow.value,
                scene = clip,
                projectId = projectId,
                stylePreset = stylePreset,
                sourceImageUri = sourceImageUri,
                styleBible = styleBible,
                prevFrameImageUri = if (chainedFromPrev) prevFrameDataUri else null,
                lastFrameImageUri = predictedEndFrameUri,
                seed = projectSeed,
                modelOverride = effectiveModel,
                aspectRatio = aspectRatio,
                durationSeconds = safeDurationPerScene,
                onTaskIdReceived = { taskId ->
                    val updatedWithTaskId = generatingClip.copy(
                        taskId = taskId,
                        statusMessage = "已接收 Task ID: $taskId，等待服务端渲染..."
                    )
                    database.sceneClipDao().updateClip(updatedWithTaskId)
                },
                onStatusUpdate = { statusMsg ->
                    onProgress(statusMsg)
                    val currentClip = database.sceneClipDao().getClipByIdDirect(clip.id) ?: generatingClip
                    database.sceneClipDao().updateClip(
                        currentClip.copy(statusMessage = statusMsg)
                    )
                }
            )

            if (clipGenResult.isSuccess) {
                val clipRes = clipGenResult.getOrThrow()
                val finalUrl = clipRes.videoUrl ?: ""
                val updatedClip = clip.copy(
                    videoUrl = finalUrl,
                    previewThumbnailUrl = finalUrl,
                    taskId = clipRes.taskId ?: clip.taskId,
                    statusMessage = clipRes.statusMessage,
                    status = GenerationStatus.COMPLETED
                )
                database.sceneClipDao().updateClip(updatedClip)
                completedClips.add(updatedClip)

                // Chain continuity: extract this clip's last frame for the next scene.
                prevFrameDataUri = agnesClient.extractLastFrameDataUri(updatedClip.videoUrl)

                database.projectDao().updateProject(
                    updatedProject.copy(
                        completedClips = completedClips.size,
                        statusMessage = "已完成分镜 ${completedClips.size}/${scenes.size} 视频生成"
                    )
                )
            } else {
                val errReason = clipGenResult.exceptionOrNull()?.message ?: "未知异常"
                val currentClip = database.sceneClipDao().getClipByIdDirect(clip.id) ?: clip
                val failedClip = currentClip.copy(
                    status = GenerationStatus.FAILED,
                    statusMessage = "生成异常: $errReason",
                    error = errReason
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

    /**
     * Normalise the planner output to exactly [targetCount] scenes, renumbered 1..N.
     *
     * - Too many scenes → keep the first [targetCount].
     * - Too few scenes → top up with curated fallback beats so the user still gets the
     *   number of clips they asked for (and never a crash from an empty list).
     * - Every scene keeps its own prompt/narration but is guaranteed a non-blank title.
     */
    private fun normalizeScenes(
        scenes: List<SceneClip>,
        targetCount: Int,
        themePrompt: String,
        stylePreset: String
    ): List<SceneClip> {
        val safeTarget = targetCount.coerceIn(MIN_SCENE_COUNT, MAX_SCENE_COUNT)
        val normalised = ArrayList<SceneClip>(safeTarget)

        for (i in 0 until safeTarget) {
            val existing = scenes.getOrNull(i)
            if (existing != null) {
                normalised.add(
                    existing.copy(
                        sceneNumber = i + 1,
                        sceneTitle = existing.sceneTitle.ifBlank { "分镜 ${i + 1}" },
                        cameraMovement = existing.cameraMovement.ifBlank { "Smooth Cinematic Pan" }
                    )
                )
            } else {
                val template = FALLBACK_SCENE_TEMPLATES[i % FALLBACK_SCENE_TEMPLATES.size]
                val camera = when (i % 4) {
                    0 -> "航拍远景下压 (Aerial Crane Down)"
                    1 -> "动态侧向跟焦 (Tracking Shot)"
                    2 -> "360度环绕升格 (360 Orbit Slow-Mo)"
                    else -> "缓慢推近特写 (Dolly-In Close-Up)"
                }
                normalised.add(
                    SceneClip(
                        projectId = "",
                        sceneNumber = i + 1,
                        sceneTitle = template.first,
                        visualPrompt = "${template.second}, style: $stylePreset, theme: $themePrompt, ultra photorealistic, 8k render, unreal engine 5 cinematics",
                        cameraMovement = camera,
                        narration = "第${i + 1}幕：${template.third}，故事在「$themePrompt」中徐徐展开。",
                        durationSeconds = 10
                    )
                )
            }
        }
        return normalised
    }

    /**
     * Re-run (re-generate) a SINGLE scene clip, regardless of whether it previously
     * succeeded or failed, then re-stitch the project master video.
     *
     * Design notes:
     * - The clip row (same [SceneClip.id]) is reset to GENERATING_CLIPS and its old
     *   result (videoUrl / taskId / error) is cleared so the UI shows a fresh run.
     * - The call is serialized by [RateLimitManager] (1 request / cooldown window), so it
     *   can safely run alongside the full pipeline without racing other API calls.
     * - Only clips that have a usable local/remote source are passed to the stitcher; a
     *   failed scene is never allowed to corrupt the master video.
     *
     * @return Result.success with the refreshed [SceneClip] on success, or
     *         Result.failure when generation fails (clip row is marked FAILED in both cases).
     */
    suspend fun rerunSceneClip(
        projectId: String,
        clipId: String,
        onProgress: (String) -> Unit = {}
    ): Result<SceneClip> {
        val project = database.projectDao().getProjectDirect(projectId)
            ?: return Result.failure(IllegalStateException("项目不存在或已被删除"))
        val clip = database.sceneClipDao().getClipByIdDirect(clipId)
            ?: return Result.failure(IllegalStateException("分镜不存在或已被删除"))

        val config = _configFlow.value
        val effectiveModel = config.videoModelName.trim().ifBlank { "agnes-video-2.5-flash" }
        val aspectRatio = project.aspectRatio.ifBlank { "16:9" }
        val stylePreset = project.stylePreset.ifBlank { "Cinematic 3D" }
        val durationSeconds = if (clip.durationSeconds > 0) clip.durationSeconds else 5

        // Reset the clip to a clean generating state (works for COMPLETED or FAILED clips alike).
        val resetClip = clip.copy(
            status = GenerationStatus.GENERATING_CLIPS,
            videoUrl = null,
            previewThumbnailUrl = null,
            taskId = null,
            error = null,
            statusMessage = "已触发单分镜重跑，准备重新提交..."
        )
        database.sceneClipDao().updateClip(resetClip)
        onProgress("正在重跑分镜 ${clip.sceneNumber}: ${clip.sceneTitle} (模型: $effectiveModel)...")

        // Continuity: if the preceding scene is already completed, chain its last frame in
        // as this clip's first frame so a re-run still blends into the surrounding shots.
        val prevFrameUri = runCatching {
            val allClips = database.sceneClipDao().getClipsForProjectDirect(projectId)
            val prev = allClips
                .filter { it.sceneNumber == clip.sceneNumber - 1 && it.status == GenerationStatus.COMPLETED }
                .maxByOrNull { it.sceneNumber }
            prev?.videoUrl?.let { agnesClient.extractLastFrameDataUri(it) }
        }.getOrNull()

        // Dual-frame control on re-run: predict this clip's end frame from its start frame so a
        // re-rendered shot still blends into the surrounding clips. Degrades to single-frame
        // control when prediction is unavailable.
        var predictedEndFrameUri: String? = null
        if (!prevFrameUri.isNullOrBlank()) {
            onProgress("正在预测分镜 ${clip.sceneNumber} 尾帧以锁定运镜轨迹...")
            val endFramePrompt = buildString {
                append("Cinematic final frame of the shot described as: ")
                append(resetClip.visualPrompt)
                if (resetClip.cameraMovement.isNotBlank()) append(", camera movement: ${resetClip.cameraMovement}")
                append(", style: $stylePreset")
                if (!project.styleBible.isNullOrBlank()) append(", consistent with: ${project.styleBible}")
                append(". This is the LAST frame, so the action has advanced to its end state while keeping the exact same subject, wardrobe, lighting and color grading as the first frame.")
            }
            predictedEndFrameUri = agnesClient.generateFrameImage(
                config = config,
                prompt = endFramePrompt,
                firstFrameDataUri = prevFrameUri,
                aspectRatio = aspectRatio
            ).getOrNull()
        }

        val genResult = agnesClient.generateSceneVideoClip(
            config = config,
            scene = resetClip,
            projectId = projectId,
            stylePreset = stylePreset,
            sourceImageUri = project.sourceImageUri,
            styleBible = project.styleBible,
            prevFrameImageUri = prevFrameUri,
            lastFrameImageUri = predictedEndFrameUri,
            seed = (projectId.hashCode().toLong() and 0x7FFFFFFFL),
            modelOverride = effectiveModel,
            aspectRatio = aspectRatio,
            durationSeconds = durationSeconds,
            onTaskIdReceived = { taskId ->
                val withTask = resetClip.copy(
                    taskId = taskId,
                    statusMessage = "已接收 Task ID: $taskId，等待服务端渲染..."
                )
                database.sceneClipDao().updateClip(withTask)
            },
            onStatusUpdate = { statusMsg ->
                onProgress(statusMsg)
                val current = database.sceneClipDao().getClipByIdDirect(clipId) ?: resetClip
                database.sceneClipDao().updateClip(current.copy(statusMessage = statusMsg))
            }
        )

        if (genResult.isFailure) {
            val errReason = genResult.exceptionOrNull()?.message ?: "未知异常"
            val current = database.sceneClipDao().getClipByIdDirect(clipId) ?: resetClip
            val failedClip = current.copy(
                status = GenerationStatus.FAILED,
                statusMessage = "重跑失败: $errReason",
                error = errReason
            )
            database.sceneClipDao().updateClip(failedClip)
            reStitchProjectIfPossible(project, onProgress)
            return Result.failure(genResult.exceptionOrNull() ?: Exception("重跑失败"))
        }

        val clipRes = genResult.getOrThrow()
        val finalUrl = clipRes.videoUrl ?: ""
        val updatedClip = resetClip.copy(
            videoUrl = finalUrl,
            previewThumbnailUrl = finalUrl,
            taskId = clipRes.taskId ?: resetClip.taskId,
            statusMessage = clipRes.statusMessage,
            status = GenerationStatus.COMPLETED,
            error = null
        )
        database.sceneClipDao().updateClip(updatedClip)
        reStitchProjectIfPossible(project, onProgress)
        return Result.success(updatedClip)
    }

    /**
     * Rebuild the master stitched video from the clips that are currently COMPLETED.
     * Silently no-ops when there is nothing usable to stitch so a single re-run never
     * destroys a previously good master video.
     */
    private suspend fun reStitchProjectIfPossible(
        project: GenerationProject,
        onProgress: (String) -> Unit
    ) {
        if (!_configFlow.value.autoStitchVideos) return

        val allClips = database.sceneClipDao().getClipsForProjectDirect(project.id)
        val completed = allClips
            .filter { it.status == GenerationStatus.COMPLETED && !it.videoUrl.isNullOrBlank() }
            .sortedBy { it.sceneNumber }
        if (completed.isEmpty()) return

        val completedCount = completed.size
        onProgress("正在用 $completedCount 段已完成分镜重新拼接长视频...")
        database.projectDao().updateProject(
            project.copy(
                completedClips = completedCount,
                status = GenerationStatus.STITCHING,
                statusMessage = "正在重新拼接所有已完成视频片段..."
            )
        )

        val stitchResult = agnesClient.stitchVideoClips(
            projectId = project.id,
            projectTitle = project.title,
            clips = completed
        )
        val finalVideoPath = stitchResult.getOrNull() ?: completed.firstOrNull()?.videoUrl
        val finalDuration = completed.sumOf { it.durationSeconds }

        database.projectDao().updateProject(
            project.copy(
                resultVideoUri = finalVideoPath,
                durationSeconds = finalDuration,
                completedClips = completedCount,
                status = GenerationStatus.COMPLETED,
                statusMessage = "已完成 $completedCount/${allClips.size} 段分镜，长视频已重新拼接"
            )
        )
    }

    suspend fun sendChatMessage(sessionId: String, userText: String, attachedImageUri: String? = null): ChatMessage {
        val userMsg = ChatMessage(
            sessionId = sessionId,
            sender = "user",
            content = userText,
            attachedImageUri = attachedImageUri
        )
        database.chatMessageDao().insertMessage(userMsg)
        // Bump recency and, on the first user turn, derive a readable conversation title.
        val session = database.chatSessionDao().getSessionById(sessionId)
        val now = System.currentTimeMillis()
        val title = if (session != null && session.title == DEFAULT_SESSION_TITLE && userText.isNotBlank()) {
            userText.trim().replace("\n", " ").take(20)
        } else {
            session?.title ?: DEFAULT_SESSION_TITLE
        }
        database.chatSessionDao().updateSessionTitle(sessionId, title, now)
        return userMsg
    }

    suspend fun saveAgentReply(
        sessionId: String,
        replyText: String,
        relatedProjectId: String? = null,
        actionType: String? = null,
        documentUri: String? = null,
        documentType: String? = null,
        documentName: String? = null,
        documentSize: String? = null
    ): ChatMessage {
        val agentMsg = ChatMessage(
            sessionId = sessionId,
            sender = "agnes_agent",
            content = replyText,
            relatedProjectId = relatedProjectId,
            actionType = actionType,
            documentUri = documentUri,
            documentType = documentType,
            documentName = documentName,
            documentSize = documentSize
        )
        database.chatMessageDao().insertMessage(agentMsg)
        database.chatSessionDao().touchSession(sessionId, System.currentTimeMillis())
        return agentMsg
    }

    /**
     * Return the most recent conversation, creating one (and adopting any pre-session rows)
     * when none exists yet. Guarantees a non-null session id for the chat screen.
     */
    suspend fun ensureActiveSession(): String {
        val existing = database.chatSessionDao().getMostRecentSession()
        if (existing != null) {
            database.chatMessageDao().assignOrphanMessages(existing.id)
            return existing.id
        }
        val session = ChatSession()
        database.chatSessionDao().insertSession(session)
        database.chatMessageDao().assignOrphanMessages(session.id)
        return session.id
    }

    /** Start a brand-new empty conversation and return its id. */
    suspend fun createChatSession(): String {
        val session = ChatSession()
        database.chatSessionDao().insertSession(session)
        return session.id
    }

    suspend fun countMessagesForSession(sessionId: String): Int =
        database.chatMessageDao().countMessagesForSession(sessionId)

    suspend fun deleteChatSession(sessionId: String) {
        database.chatMessageDao().deleteMessagesForSession(sessionId)
        database.chatSessionDao().deleteSessionById(sessionId)
    }

    suspend fun deleteProject(project: GenerationProject) {
        database.sceneClipDao().deleteClipsForProject(project.id)
        database.projectDao().deleteProject(project)
    }

    suspend fun clearChatHistory() {
        database.chatMessageDao().clearAllMessages()
    }}
