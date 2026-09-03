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
    ).fallbackToDestructiveMigration().build()

    private val rateLimitManager = RateLimitManager(cooldownIntervalSeconds = 60)
    private val agnesClient = AgnesClient(application, rateLimitManager)
    val repository = AgnesRepository(application, database, rateLimitManager, agnesClient)

    val config: StateFlow<AgnesApiConfig> = repository.configFlow
    val rateLimitState: StateFlow<RateLimitState> = repository.rateLimitState

    val projects: StateFlow<List<GenerationProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                        replyText = "你好！我是你的 Agnes AI 智能体助手 🎬✨\n\n你可以：\n1️⃣ 上传参考图或直接描述，为你生成震撼的全新 AI 图像\n2️⃣ 上传图片或故事构思，为你自动拆解多段电影分镜脚本、排队生成视频片段并一键拼接完整长视频！\n\n⚠️ 温馨提示：Agnes API 每分钟严格限速 1 次调用，我已为你配备了全自动智能队列与秒级冷却倒计时调度器，保证稳定不超频。"
                    )
                }
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

            // Intelligent Agnes Agent Dispatcher
            val lower = text.lowercase()
            if (lower.contains("视频") || lower.contains("短片") || lower.contains("分镜") || lower.contains("拼接") || lower.contains("video")) {
                repository.saveAgentReply(
                    replyText = "收到你的视频生成指令！我正在根据你上传的图片/构思规划分镜脚本，并将依次生成各分段视频后无缝拼接。请在限速队列中查看实时生成进度。",
                    actionType = "VIDEO_SCRIPT"
                )
                startVideoPipeline(
                    themePrompt = if (text.isNotBlank()) text else "根据参考图生成的电影级连续故事",
                    sourceImageUri = attachedImageUri,
                    sceneCount = 4,
                    stylePreset = "Cinematic 3D"
                )
            } else {
                repository.saveAgentReply(
                    replyText = "收到！已接收你的指令与参考图片，正在通过 Agnes API 调度队列进行富有创意的图像重绘变奏...",
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
                                replyText = "✨ 为你生成的新创意图片已就绪！Agnes AI 已依据参考图的结构、色彩与风格完成重绘创作。",
                                relatedProjectId = proj.id,
                                actionType = "IMAGE_RESULT"
                            )
                        }
                    }
                )
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
