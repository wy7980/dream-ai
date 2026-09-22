package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.api.AgnesClient
import com.example.data.api.RateLimitManager
import com.example.data.local.AppDatabase
import com.example.data.model.AIProvider
import com.example.data.model.AgnesApiConfig
import com.example.data.model.ChatMessage
import com.example.data.model.GenerationProject
import com.example.data.model.ProjectType
import com.example.data.model.RateLimitState
import com.example.data.model.SceneClip
import com.example.data.repository.AgnesRepository
import com.example.data.skill.AgentDecision
import com.example.data.skill.AgentDecisionEngine
import com.example.data.skill.AgentSkill
import com.example.data.skill.AgentSkillRegistry
import com.example.data.skill.InvocationStatus
import com.example.data.skill.SkillExecutionContext
import com.example.data.skill.SkillInvocationRecord
import com.example.util.DocumentExportHelper
import com.example.util.DocumentType
import com.example.util.GeneratedDocument
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgnesViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "agnes_studio_db"
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    private val rateLimitManager = RateLimitManager(cooldownIntervalSeconds = 60)
    private val agnesClient = AgnesClient(application, rateLimitManager)
    val repository = AgnesRepository(application, database, rateLimitManager, agnesClient)

    val config: StateFlow<AgnesApiConfig> = repository.configFlow
    val rateLimitState: StateFlow<RateLimitState> = repository.rateLimitState

    val projects: StateFlow<List<GenerationProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _availableModels = MutableStateFlow<List<String>>(agnesClient.defaultPresetModels)
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    private val _chatIntentMode = MutableStateFlow(com.example.data.model.ChatIntentMode.AUTO)
    val chatIntentMode: StateFlow<com.example.data.model.ChatIntentMode> = _chatIntentMode.asStateFlow()

    // Agent Skills System
    val skillRegistry = AgentSkillRegistry(application, repository, agnesClient)
    private val decisionEngine = AgentDecisionEngine(skillRegistry, agnesClient)

    val skills: StateFlow<List<AgentSkill>> = skillRegistry.skills

    private val _currentExecutingSkill = MutableStateFlow<SkillInvocationRecord?>(null)
    val currentExecutingSkill: StateFlow<SkillInvocationRecord?> = _currentExecutingSkill.asStateFlow()

    private val _selectedProject = MutableStateFlow<GenerationProject?>(null)
    val selectedProject: StateFlow<GenerationProject?> = _selectedProject.asStateFlow()

    private val _selectedProjectClips = MutableStateFlow<List<SceneClip>>(emptyList())
    val selectedProjectClips: StateFlow<List<SceneClip>> = _selectedProjectClips.asStateFlow()

    // The active collector for the selected project's clips. Kept so we can cancel the previous
    // one on every selection — otherwise stale collectors from other projects keep pushing their
    // clips into _selectedProjectClips (Room re-emits on any write), making the studio flip
    // between tasks whenever a background generation or re-run touches another project.
    private var selectedClipsJob: Job? = null

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isVideoGenerating = MutableStateFlow(false)
    val isVideoGenerating: StateFlow<Boolean> = _isVideoGenerating.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _videoProgressMessage = MutableStateFlow("")
    val videoProgressMessage: StateFlow<String> = _videoProgressMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var chatJob: kotlinx.coroutines.Job? = null
    private var videoJob: kotlinx.coroutines.Job? = null
    private var rerunJob: kotlinx.coroutines.Job? = null

    private val _rerunningClipId = MutableStateFlow<String?>(null)
    val rerunningClipId: StateFlow<String?> = _rerunningClipId.asStateFlow()

    fun cancelChatTask() {
        if (chatJob?.isActive == true) {
            chatJob?.cancel()
            chatJob = null
            _isGenerating.value = false
            _progressMessage.value = ""
            _currentExecutingSkill.value = _currentExecutingSkill.value?.copy(
                status = InvocationStatus.FAILED,
                statusMessage = "任务已被用户主动终止"
            )
            _toastMessage.value = "已终止智能体当前任务"
        }
    }

    fun cancelVideoTask() {
        if (videoJob?.isActive == true) {
            videoJob?.cancel()
            videoJob = null
            _isVideoGenerating.value = false
            _videoProgressMessage.value = ""
            _toastMessage.value = "已终止视频后台渲染任务"
        }
    }

    init {
        // Add welcome message if chat is empty
        viewModelScope.launch {
            repository.chatMessages.collect { list ->
                if (list.isEmpty()) {
                    repository.saveAgentReply(
                        replyText = "你好！我是 **Dream AI 创作智能体** 🧠⚡\n\n我已支持自主加载与调度多种专业技能（Skills）：\n- 🎨 **AI 图像生成与风格重绘**：基于文本或参考图生成高清画作、壁纸与艺术变奏\n- 🎬 **AI 电影分镜与视频流水线**：全自动影视分镜规划、多段视频逐幕渲染与无缝拼接\n- ✨ **视觉提示词专家润色**：中英文专业摄影级光影构图与渲染材质提示词\n- 📋 **导演级分镜规划**：好莱坞工业标准视听镜头语言设计与剧本拆解\n\n你可以通过自然语言直接向我提问或下达创作指令，我将自主识别并调用相应 Skill 执行任务！"
                    )
                }
            }
        }
    }

    fun toggleSkill(skillId: String, enabled: Boolean) {
        skillRegistry.setSkillEnabled(skillId, enabled)
        val stateText = if (enabled) "已启用" else "已停用"
        _toastMessage.value = "技能 [$skillId] $stateText"
    }

    fun setChatIntentMode(mode: com.example.data.model.ChatIntentMode) {
        _chatIntentMode.value = mode
    }

    fun fetchModelsFromEndpoint(onResult: (Boolean, Int, String) -> Unit = { _, _, _ -> }) {
        viewModelScope.launch {
            _isFetchingModels.value = true
            val result = repository.fetchRemoteModels()
            _isFetchingModels.value = false
            if (result.isSuccess) {
                val list = result.getOrThrow()
                _availableModels.value = list
                onResult(true, list.size, "成功拉取到 ${list.size} 个可用模型")
            } else {
                onResult(false, _availableModels.value.size, result.exceptionOrNull()?.message ?: "拉取失败，已使用内置推荐模型")
            }
        }
    }

    fun fetchModelsForProvider(provider: AIProvider, onResult: (Boolean, List<String>, String) -> Unit = { _, _, _ -> }) {
        viewModelScope.launch {
            _isFetchingModels.value = true
            val result = repository.fetchModelsForProvider(provider)
            _isFetchingModels.value = false
            if (result.isSuccess) {
                val list = result.getOrThrow()
                // Update the provider in the config with newly fetched custom models
                val updatedProviders = config.value.providers.map {
                    if (it.id == provider.id) it.copy(customModels = list) else it
                }
                updateConfig(config.value.copy(providers = updatedProviders))
                _availableModels.value = list
                onResult(true, list, "成功拉取到 ${list.size} 个模型")
            } else {
                val fallbackList = provider.customModels.ifEmpty { _availableModels.value }
                onResult(false, fallbackList, result.exceptionOrNull()?.message ?: "拉取失败")
            }
        }
    }

    fun testProviderConnection(provider: AIProvider, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _progressMessage.value = "正在测试连接 ${provider.name}..."
            val result = repository.testProviderConnection(provider)
            if (result.isSuccess) {
                onResult(true, result.getOrNull() ?: "连接成功")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "连接失败")
            }
            _progressMessage.value = ""
        }
    }

    fun addOrUpdateProvider(provider: AIProvider) {
        val currentList = config.value.providers.toMutableList()
        val index = currentList.indexOfFirst { it.id == provider.id }
        if (index != -1) {
            currentList[index] = provider
        } else {
            currentList.add(provider)
        }
        val updated = config.value.copy(providers = currentList)
        updateConfig(updated)
        _toastMessage.value = "已保存 Provider: ${provider.name}"
    }

    fun deleteProvider(providerId: String) {
        if (config.value.providers.size <= 1) {
            _toastMessage.value = "至少保留一个 Provider 渠道"
            return
        }
        val currentList = config.value.providers.filter { it.id != providerId }
        val fallbackId = currentList.first().id
        val updated = config.value.copy(
            providers = currentList,
            chatProviderId = if (config.value.chatProviderId == providerId) fallbackId else config.value.chatProviderId,
            imageProviderId = if (config.value.imageProviderId == providerId) fallbackId else config.value.imageProviderId,
            videoProviderId = if (config.value.videoProviderId == providerId) fallbackId else config.value.videoProviderId
        )
        updateConfig(updated)
        _toastMessage.value = "Provider 已移除"
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun toggleTheme(isDark: Boolean) {
        val updated = config.value.copy(isDarkTheme = isDark)
        repository.saveConfig(updated)
        _toastMessage.value = if (isDark) "已切换为暗黑模式" else "已切换为明亮模式"
    }

    fun updateConfig(newConfig: AgnesApiConfig) {
        repository.saveConfig(newConfig)
        _toastMessage.value = "Agnes API 配置已保存！限速周期: ${newConfig.rateLimitSeconds}秒"
    }

    fun testApiConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _progressMessage.value = "正在测试连接 Agnes API..."
            val result = repository.testConnection()
            if (result.isSuccess) {
                onResult(true, result.getOrNull() ?: "连接成功")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "连接失败")
            }
            _progressMessage.value = ""
        }
    }

    fun generateImage(
        prompt: String,
        stylePreset: String,
        aspectRatio: String,
        sourceImageUri: String?,
        onSuccess: (GenerationProject) -> Unit = {}
    ) {
        if (prompt.isBlank() && sourceImageUri == null) {
            _toastMessage.value = "请输入图片生成描述或上传参考图片"
            return
        }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _progressMessage.value = "排队等待 Agnes API 调度中..."
                
                val result = repository.generateImageToImage(
                    prompt = prompt,
                    stylePreset = stylePreset,
                    aspectRatio = aspectRatio,
                    sourceImageUri = sourceImageUri
                )

                if (result.isSuccess) {
                    val proj = result.getOrThrow()
                    _selectedProject.value = proj
                    _toastMessage.value = "AI 图像生成完成！"
                    onSuccess(proj)
                } else {
                    _toastMessage.value = "生成失败: ${result.exceptionOrNull()?.message}"
                }
            } finally {
                _isGenerating.value = false
                _progressMessage.value = ""
            }
        }
    }

    fun startVideoPipeline(
        themePrompt: String,
        sourceImageUri: String?,
        sceneCount: Int = 1,
        stylePreset: String = "Cinematic 3D",
        videoModel: String? = null,
        aspectRatio: String = "16:9",
        durationPerScene: Int = 5,
        onSuccess: (GenerationProject) -> Unit = {}
    ) {
        if (themePrompt.isBlank() && sourceImageUri == null) {
            _toastMessage.value = "请输入视频主题或上传参考图片"
            return
        }

        // Defensive clamp: the UI only offers 1..20, but the agent/skill path may call in
        // directly, and each scene costs one rate-limited video request.
        val safeSceneCount = sceneCount.coerceIn(
            AgnesRepository.MIN_SCENE_COUNT,
            AgnesRepository.MAX_SCENE_COUNT
        )

        videoJob?.cancel()
        videoJob = viewModelScope.launch {
            try {
                _isVideoGenerating.value = true
                _videoProgressMessage.value = "正在启动多段视频生成流水线..."

                val result = repository.startFullVideoPipeline(
                    themePrompt = themePrompt,
                    sourceImageUri = sourceImageUri,
                    sceneCount = safeSceneCount,
                    stylePreset = stylePreset,
                    videoModel = videoModel,
                    aspectRatio = aspectRatio,
                    durationPerScene = durationPerScene,
                    onProgress = { msg ->
                        _videoProgressMessage.value = msg
                    }
                )

                if (result.isSuccess) {
                    val proj = result.getOrThrow()
                    selectProject(proj)
                    _toastMessage.value = "视频流水线已完成！多段视频已拼接合成。"
                    onSuccess(proj)
                } else {
                    _toastMessage.value = "视频生成失败: ${result.exceptionOrNull()?.message}"
                }
            } finally {
                _isVideoGenerating.value = false
                _videoProgressMessage.value = ""
            }
        }
    }

    /**
     * Persist user edits to a storyboard scene's creative fields. This is a lightweight
     * write that never re-renders the clip: the user can fix a prompt first and then hit
     * "重跑本分镜" to regenerate with the corrected text.
     */
    fun updateScenePrompt(
        clipId: String,
        title: String,
        visualPrompt: String,
        cameraMovement: String,
        narration: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (clipId.isBlank()) {
            onComplete(false, "分镜不存在")
            return
        }
        if (visualPrompt.isBlank()) {
            _toastMessage.value = "分镜提示词不能为空"
            onComplete(false, "分镜提示词不能为空")
            return
        }
        viewModelScope.launch {
            val result = repository.updateClipPrompt(
                clipId = clipId,
                title = title,
                visualPrompt = visualPrompt,
                cameraMovement = cameraMovement,
                narration = narration
            )
            if (result.isSuccess) {
                _toastMessage.value = "分镜脚本已保存"
                onComplete(true, "已保存")
            } else {
                val err = result.exceptionOrNull()?.message ?: "未知错误"
                _toastMessage.value = "分镜脚本保存失败: $err"
                onComplete(false, err)
            }
        }
    }

    /**
     * Re-run a SINGLE scene clip (works for both COMPLETED and FAILED clips) and refresh
     * the project's master video afterwards.
     */
    fun rerunSceneClip(projectId: String, clipId: String) {
        if (_rerunningClipId.value != null) {
            _toastMessage.value = "已有分镜正在重跑，请稍候..."
            return
        }
        rerunJob = viewModelScope.launch {
            _rerunningClipId.value = clipId
            _videoProgressMessage.value = "正在重跑单个分镜..."
            try {
                val result = repository.rerunSceneClip(
                    projectId = projectId,
                    clipId = clipId,
                    onProgress = { msg -> _videoProgressMessage.value = msg }
                )
                if (result.isSuccess) {
                    _toastMessage.value = "分镜重跑成功，长视频已更新！"
                } else {
                    _toastMessage.value = "分镜重跑失败: ${result.exceptionOrNull()?.message}"
                }
            } finally {
                _rerunningClipId.value = null
                _videoProgressMessage.value = ""
                // Refresh the selected project snapshot so the player reflects the new master video.
                _selectedProject.value?.let { current ->
                    repository.getProjectDirect(current.id)?.let { fresh ->
                        _selectedProject.value = fresh
                    }
                }
            }
        }
    }

    fun sendUserMessage(text: String, attachedImageUri: String?) {
        if (text.isBlank() && attachedImageUri == null) return

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                repository.sendChatMessage(text, attachedImageUri)

                val mode = _chatIntentMode.value
                _isGenerating.value = true
                _progressMessage.value = "Dream AI 智能体决策中..."

                val decision = decisionEngine.decide(
                    config = config.value,
                    userPrompt = text,
                    attachedImageUri = attachedImageUri,
                    chatHistory = chatMessages.value,
                    mode = mode
                )

                when (decision) {
                    is AgentDecision.DirectChatReply -> {
                        _isGenerating.value = false
                        _progressMessage.value = ""
                        repository.saveAgentReply(
                            replyText = decision.replyText,
                            actionType = "CHAT_REPLY"
                        )
                    }
                    is AgentDecision.InvokeSkill -> {
                        executeSkillInternal(
                            skill = decision.skill,
                            arguments = decision.arguments,
                            preThoughtText = decision.preThoughtText,
                            attachedImageUri = attachedImageUri
                        )
                    }
                }
            } finally {
                _isGenerating.value = false
                _progressMessage.value = ""
            }
        }
    }

    /**
     * Directly invoke a loaded skill with arguments.
     */
    fun invokeSkillManually(skillId: String, arguments: Map<String, Any?>, attachedImageUri: String? = null) {
        val skill = skillRegistry.getSkill(skillId) ?: return
        if (!skill.isEnabled) {
            _toastMessage.value = "技能 [${skill.name}] 当前处于停用状态，请先在技能中心开启。"
            return
        }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                val userText = "调用技能 [${skill.name}]"
                repository.sendChatMessage(userText, attachedImageUri)
                executeSkillInternal(
                    skill = skill,
                    arguments = arguments,
                    preThoughtText = "⚡ [用户手动唤起技能] 已加载技能 `[${skill.name}]`。",
                    attachedImageUri = attachedImageUri
                )
            } finally {
                _isGenerating.value = false
                _progressMessage.value = ""
            }
        }
    }

    private suspend fun executeSkillInternal(
        skill: AgentSkill,
        arguments: Map<String, Any?>,
        preThoughtText: String,
        attachedImageUri: String?
    ) {
        val startTime = System.currentTimeMillis()
        _isGenerating.value = true
        _progressMessage.value = "正在加载技能: ${skill.name}..."

        // Record executing state
        _currentExecutingSkill.value = SkillInvocationRecord(
            skillId = skill.id,
            skillName = skill.name,
            iconEmoji = skill.iconEmoji,
            arguments = arguments,
            status = InvocationStatus.EXECUTING,
            statusMessage = "正在执行技能: ${skill.name}..."
        )

        // Save pre-thought message to chat stream
        val formattedArgs = arguments.entries.joinToString(", ") { "${it.key}: \"${it.value}\"" }
        repository.saveAgentReply(
            replyText = "$preThoughtText\n\n⚡ **已装载并调用技能**：`${skill.name}` ${skill.iconEmoji}\n> 入参规格: `{$formattedArgs}`",
            actionType = "SKILL_CALL"
        )

        val executionContext = SkillExecutionContext(
            config = config.value,
            attachedImageUri = attachedImageUri,
            onProgress = { step ->
                _progressMessage.value = step
                _currentExecutingSkill.value = _currentExecutingSkill.value?.copy(
                    statusMessage = step
                )
            }
        )

        val result = skill.execute(executionContext, arguments)
        val elapsed = System.currentTimeMillis() - startTime
        _isGenerating.value = false
        _progressMessage.value = ""

        if (result.success) {
            _currentExecutingSkill.value = _currentExecutingSkill.value?.copy(
                status = InvocationStatus.SUCCESS,
                statusMessage = "技能执行成功 (${elapsed}ms)",
                executionTimeMs = elapsed,
                resultSummary = result.outputMessage,
                relatedProjectId = result.relatedProjectId
            )

            // If a project was generated, update selected project
            if (result.relatedProjectId != null) {
                val proj = repository.getProjectDirect(result.relatedProjectId)
                if (proj != null) {
                    selectProject(proj)
                }
            }

            val stepsFormatted = if (result.intermediateSteps.isNotEmpty()) {
                "\n\n**工序节点**:\n" + result.intermediateSteps.joinToString("\n") { "✓ $it" }
            } else ""

            val actionType = when {
                skill.id == "image-generation" -> "IMAGE_RESULT"
                skill.id == "video-generation" -> "VIDEO_SCRIPT"
                result.outputDocumentUri != null -> "DOCUMENT_RESULT"
                else -> "SKILL_COMPLETED"
            }

            repository.saveAgentReply(
                replyText = "${result.outputMessage}$stepsFormatted",
                relatedProjectId = result.relatedProjectId,
                actionType = actionType,
                documentUri = result.outputDocumentUri,
                documentType = result.outputDocumentType,
                documentName = result.outputDocumentName,
                documentSize = result.outputDocumentSize
            )
        } else {
            _currentExecutingSkill.value = _currentExecutingSkill.value?.copy(
                status = InvocationStatus.FAILED,
                statusMessage = result.error ?: "执行失败",
                executionTimeMs = elapsed
            )
            repository.saveAgentReply(
                replyText = "⚠️ 技能 `[${skill.name}]` 执行未完成: ${result.error ?: "未知错误"}",
                actionType = "SKILL_FAILED"
            )
        }
    }

    fun openDocument(uriString: String?, docType: String?) {
        if (uriString.isNullOrBlank()) {
            _toastMessage.value = "未找到有效的文档路径"
            return
        }
        val mimeType = when (docType?.uppercase()) {
            "WORD" -> "application/msword"
            "PDF" -> "application/pdf"
            "EXCEL" -> "text/csv"
            else -> "*/*"
        }
        val opened = DocumentExportHelper.openDocument(
            context = getApplication(),
            docUri = Uri.parse(uriString),
            mimeType = mimeType
        )
        if (!opened) {
            _toastMessage.value = "未检测到可直接打开该格式的应用，建议安装 WPS 或 Office"
        }
    }

    fun shareDocument(uriString: String?, docName: String?, docType: String?) {
        if (uriString.isNullOrBlank()) {
            _toastMessage.value = "未找到可分享的文档"
            return
        }
        val mimeType = when (docType?.uppercase()) {
            "WORD" -> "application/msword"
            "PDF" -> "application/pdf"
            "EXCEL" -> "text/csv"
            else -> "*/*"
        }
        DocumentExportHelper.shareDocument(
            context = getApplication(),
            docUri = Uri.parse(uriString),
            mimeType = mimeType,
            title = docName ?: "导出文档"
        )
    }

    fun saveDocumentToDownloads(docUriString: String?, docName: String?, docType: String?) {
        if (docUriString.isNullOrBlank()) {
            _toastMessage.value = "未找到文档"
            return
        }
        viewModelScope.launch {
            try {
                val dir = File(getApplication<Application>().filesDir, "generated_docs")
                val fileName = docName ?: "document"
                val file = File(dir, fileName)
                if (file.exists()) {
                    val mimeType = when (docType?.uppercase()) {
                        "WORD" -> "application/msword"
                        "PDF" -> "application/pdf"
                        "EXCEL" -> "text/csv"
                        else -> "application/octet-stream"
                    }
                    val doc = GeneratedDocument(
                        file = file,
                        uri = Uri.parse(docUriString),
                        title = docName ?: "文档",
                        fileName = fileName,
                        fileSizeBytes = file.length(),
                        formattedSize = DocumentExportHelper.formatFileSize(file.length()),
                        mimeType = mimeType,
                        docType = when (docType?.uppercase()) {
                            "WORD" -> DocumentType.WORD
                            "PDF" -> DocumentType.PDF
                            "EXCEL" -> DocumentType.EXCEL
                            else -> DocumentType.WORD
                        }
                    )
                    val res = DocumentExportHelper.saveToDownloads(getApplication(), doc)
                    if (res.isSuccess) {
                        _toastMessage.value = "已将 ${doc.fileName} 保存到系统 Downloads/DreamAI 目录"
                    } else {
                        _toastMessage.value = "保存失败: ${res.exceptionOrNull()?.message}"
                    }
                } else {
                    _toastMessage.value = "本地暂存文件未找到"
                }
            } catch (e: Exception) {
                _toastMessage.value = "保存失败: ${e.message}"
            }
        }
    }

    fun selectProject(project: GenerationProject?) {
        // Drop the previous project's collector before switching, so it can never overwrite the
        // clip list for the newly selected project (fixes content jumping between tasks).
        selectedClipsJob?.cancel()
        selectedClipsJob = null
        _selectedProject.value = project
        // Clear immediately so the previous task's clips never linger on screen while the new
        // collector warms up (Room re-emits asynchronously).
        _selectedProjectClips.value = emptyList()
        if (project != null) {
            selectedClipsJob = viewModelScope.launch {
                repository.getClipsForProject(project.id).collect { clips ->
                    _selectedProjectClips.value = clips
                }
            }
        } else {
            _selectedProjectClips.value = emptyList()
        }
    }

    fun deleteProject(project: GenerationProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = null
                _selectedProjectClips.value = emptyList()
            }
            _toastMessage.value = "项目已删除"
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _toastMessage.value = "聊天记录已清空"
        }
    }

    fun saveImageToGallery(imageUriOrPath: String?, title: String? = null, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (imageUriOrPath.isNullOrBlank()) {
            _toastMessage.value = "图像路径为空，无法保存"
            onComplete(false, "图像路径为空")
            return
        }

        viewModelScope.launch {
            _toastMessage.value = "正在保存高清图像到系统相册..."
            val result = com.example.util.MediaExportHelper.saveImageToGallery(
                context = getApplication(),
                imageUriOrPath = imageUriOrPath,
                title = title
            )
            if (result.isSuccess) {
                val uri = result.getOrThrow()
                _toastMessage.value = "🎨 图像已成功保存至系统相册！(Pictures/AgnesAI)"
                onComplete(true, "保存成功: $uri")
            } else {
                val err = result.exceptionOrNull()?.message ?: "未知错误"
                _toastMessage.value = "保存失败: $err"
                onComplete(false, err)
            }
        }
    }

    fun exportVideoProject(project: GenerationProject, clips: List<SceneClip>, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _toastMessage.value = "正在导出并保存视频分镜至系统媒体库..."
            val result = com.example.util.MediaExportHelper.saveVideoProjectToGallery(
                context = getApplication(),
                project = project,
                clips = clips
            )
            if (result.isSuccess) {
                val uri = result.getOrThrow()
                _toastMessage.value = "🎬 完整分镜长视频/图谱已成功保存至系统媒体库！"
                onComplete(true, "导出成功: $uri")
            } else {
                val err = result.exceptionOrNull()?.message ?: "未知错误"
                _toastMessage.value = "导出失败: $err"
                onComplete(false, err)
            }
        }
    }

    fun updateTavilyApiKey(key: String) {
        val current = config.value
        updateConfig(current.copy(tavilyApiKey = key.trim()))
        _toastMessage.value = "已更新 Tavily API Key 配置"
    }

    fun testTavilyConnection(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isGenerating.value = true
            _progressMessage.value = "正在验证 Tavily API 密钥与检索通道..."
            val result = agnesClient.testTavilyConnection(key.trim())
            _isGenerating.value = false
            _progressMessage.value = ""
            if (result.isSuccess) {
                val msg = result.getOrThrow()
                _toastMessage.value = msg
                onResult(true, msg)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Tavily 连通测试失败"
                _toastMessage.value = "❌ 测试失败: $err"
                onResult(false, err)
            }
        }
    }

    fun shareMedia(uriOrPath: String?, isVideo: Boolean = false) {
        com.example.util.MediaExportHelper.shareMedia(
            context = getApplication(),
            uriOrPath = uriOrPath,
            isVideo = isVideo
        )
    }
}
