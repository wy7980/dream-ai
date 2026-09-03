package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.api.AgnesClient
import com.example.data.api.RateLimitManager
import com.example.data.local.AppDatabase
import com.example.data.model.AgnesApiConfig
import com.example.data.model.ChatMessage
import com.example.data.model.GenerationProject
import com.example.data.model.ProjectType
import com.example.data.model.RateLimitState
import com.example.data.model.SceneClip
import com.example.data.repository.AgnesRepository
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

    private val _selectedProject = MutableStateFlow<GenerationProject?>(null)
    val selectedProject: StateFlow<GenerationProject?> = _selectedProject.asStateFlow()

    private val _selectedProjectClips = MutableStateFlow<List<SceneClip>>(emptyList())
    val selectedProjectClips: StateFlow<List<SceneClip>> = _selectedProjectClips.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Add welcome message if chat is empty
        viewModelScope.launch {
            repository.chatMessages.collect { list ->
                if (list.isEmpty()) {
                    repository.saveAgentReply(
                        replyText = "你好！我是你的 Agnes AI 全能创作智能体 🎬✨\n\n你可以和我：\n💬 **自由畅聊**：探讨创意构想、润色提示词（对话模型拥有更高吞吐速率，无需排队）\n🎨 **智能生图 / 重绘**：描述画面或发送参考图，自动调用生图模型（1分钟限速保护）\n🎬 **分镜生视频**：一句话生成多幕电影短片并自动拼接成片（1分钟限速保护）\n\n可在输入框上方切换专属模式，或在「设置」中自动拉取模型列表！"
                    )
                }
            }
        }
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

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
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

        viewModelScope.launch {
            _isGenerating.value = true
            _progressMessage.value = "排队等待 Agnes API 调度中..."
            
            val result = repository.generateImageToImage(
                prompt = prompt,
                stylePreset = stylePreset,
                aspectRatio = aspectRatio,
                sourceImageUri = sourceImageUri
            )

            _isGenerating.value = false
            _progressMessage.value = ""

            if (result.isSuccess) {
                val proj = result.getOrThrow()
                _selectedProject.value = proj
                _toastMessage.value = "AI 图像生成完成！"
                onSuccess(proj)
            } else {
                _toastMessage.value = "生成失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun startVideoPipeline(
        themePrompt: String,
        sourceImageUri: String?,
        sceneCount: Int = 4,
        stylePreset: String = "Cinematic 3D",
        onSuccess: (GenerationProject) -> Unit = {}
    ) {
        if (themePrompt.isBlank() && sourceImageUri == null) {
            _toastMessage.value = "请输入视频主题或上传参考图片"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _progressMessage.value = "正在启动多段视频生成流水线..."

            val result = repository.startFullVideoPipeline(
                themePrompt = themePrompt,
                sourceImageUri = sourceImageUri,
                sceneCount = sceneCount,
                stylePreset = stylePreset,
                onProgress = { msg ->
                    _progressMessage.value = msg
                }
            )

            _isGenerating.value = false
            _progressMessage.value = ""

            if (result.isSuccess) {
                val proj = result.getOrThrow()
                selectProject(proj)
                _toastMessage.value = "视频流水线已完成！多段视频已拼接合成。"
                onSuccess(proj)
            } else {
                _toastMessage.value = "视频生成失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun sendUserMessage(text: String, attachedImageUri: String?) {
        if (text.isBlank() && attachedImageUri == null) return

        viewModelScope.launch {
            repository.sendChatMessage(text, attachedImageUri)

            val mode = _chatIntentMode.value
            val lower = text.lowercase()

            val isVideoIntent = mode == com.example.data.model.ChatIntentMode.VIDEO_GEN ||
                    (mode == com.example.data.model.ChatIntentMode.AUTO && (
                            lower.contains("视频") || lower.contains("短片") ||
                                    lower.contains("分镜") || lower.contains("拼接") ||
                                    lower.contains("video") || lower.contains("movie") || lower.contains("生成短片")
                            ))

            val isImageIntent = mode == com.example.data.model.ChatIntentMode.IMAGE_GEN ||
                    (mode == com.example.data.model.ChatIntentMode.AUTO && (
                            attachedImageUri != null ||
                                    lower.contains("画") || lower.contains("生图") ||
                                    lower.contains("重绘") || lower.contains("变奏") ||
                                    lower.contains("图片") || lower.contains("插画") ||
                                    lower.contains("壁纸") || lower.contains("image") || lower.contains("draw")
                            ))

            when {
                isVideoIntent -> {
                    // Task 1: Video Generation (Video Model with strict 60s cooldown per clip)
                    repository.saveAgentReply(
                        replyText = "🎬 收到你的视频生成指令！我正在调用视频生成模型（${config.value.videoModelName}）规划电影分镜脚本，并将在 1 分钟限速调度队列中逐段生成并自动拼接。",
                        actionType = "VIDEO_SCRIPT"
                    )
                    startVideoPipeline(
                        themePrompt = if (text.isNotBlank()) text else "基于参考画面的电影级多幕视频短片",
                        sourceImageUri = attachedImageUri,
                        sceneCount = 4,
                        stylePreset = "Cinematic 3D"
                    )
                }
                isImageIntent -> {
                    // Task 2: Image Generation / Remix (Image Model with strict 60s cooldown)
                    repository.saveAgentReply(
                        replyText = "🎨 收到！已调度图像生成模型（${config.value.modelName}），正在 1 分钟限速保护队列中为你进行高清重绘与创意变奏...",
                        actionType = "GENERATING"
                    )
                    generateImage(
                        prompt = if (text.isNotBlank()) text else "基于参考图片的艺术变奏创作",
                        stylePreset = "Cinematic 3D",
                        aspectRatio = "16:9",
                        sourceImageUri = attachedImageUri,
                        onSuccess = { proj ->
                            viewModelScope.launch {
                                repository.saveAgentReply(
                                    replyText = "✨ 为你生成的新创意图片已就绪！已依据构思与参考图完成高清重绘。",
                                    relatedProjectId = proj.id,
                                    actionType = "IMAGE_RESULT"
                                )
                            }
                        }
                    )
                }
                else -> {
                    // Task 3: Conversational Chat (Chat Model with high rate limit, fast & non-blocking)
                    val history = chatMessages.value
                    val replyResult = repository.generateChatReply(history, text)
                    val replyContent = replyResult.getOrNull() ?: "你好！我是你的 Agnes AI 智能助手，有什么我可以协助你的？"
                    repository.saveAgentReply(
                        replyText = replyContent,
                        actionType = "CHAT_REPLY"
                    )
                }
            }
        }
    }

    fun selectProject(project: GenerationProject?) {
        _selectedProject.value = project
        if (project != null) {
            viewModelScope.launch {
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
}
