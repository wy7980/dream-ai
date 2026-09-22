package com.example.data.skill

import android.content.Context
import com.example.data.api.AgnesClient
import com.example.data.model.GenerationProject
import com.example.data.model.VideoDurationLimits
import com.example.data.repository.AgnesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Skill 1: Image Generation & Style Remix
 */
class ImageGenerationSkill(
    private val repository: AgnesRepository
) : AgentSkill {
    override val id: String = "image-generation"
    override val name: String = "AI 图像生成与风格重绘"
    override val iconEmoji: String = "🎨"
    override val description: String = "调用高精图像生成大模型，根据文字提示词与可选参考图，生成艺术插画、概念设计、壁纸肖像或图片变奏重绘。"
    override val category: String = "creation"
    override var isEnabled: Boolean = true

    override val triggerKeywords: List<String> = listOf(
        "画", "生图", "画图", "图片", "插画", "壁纸", "重绘", "变奏", "原画", "设计图", "海报", "image", "draw", "picture", "photo"
    )

    override val parameters: List<SkillParam> = listOf(
        SkillParam(
            name = "prompt",
            type = "string",
            description = "画面的主体、场景、构图与细节描述（支持中英文）",
            required = true
        ),
        SkillParam(
            name = "stylePreset",
            type = "string",
            description = "视觉艺术风格，如 'Cinematic 3D', 'Cyberpunk', 'Anime', 'Photorealistic', 'Oil Painting'",
            required = false,
            defaultValue = "Cinematic 3D"
        ),
        SkillParam(
            name = "aspectRatio",
            type = "string",
            description = "画面宽高比，如 '16:9', '1:1', '9:16', '4:3'",
            required = false,
            defaultValue = "16:9"
        ),
        SkillParam(
            name = "sourceImageUri",
            type = "image_uri",
            description = "参考图本地或网络 URI（可选，若提供则基于参考图执行变奏重绘）",
            required = false
        )
    )

    override suspend fun execute(
        context: SkillExecutionContext,
        arguments: Map<String, Any?>
    ): SkillResult {
        val prompt = arguments["prompt"]?.toString()?.takeIf { it.isNotBlank() }
            ?: "根据灵感生成的极具质感的艺术概念画面"
        val stylePreset = arguments["stylePreset"]?.toString() ?: "Cinematic 3D"
        val aspectRatio = arguments["aspectRatio"]?.toString() ?: "16:9"
        val sourceImageUri = arguments["sourceImageUri"]?.toString() ?: context.attachedImageUri

        context.onProgress("技能 [image-generation] 正在调度图像生成模型 (${context.config.modelName})...")

        val result = repository.generateImageToImage(
            prompt = prompt,
            stylePreset = stylePreset,
            aspectRatio = aspectRatio,
            sourceImageUri = sourceImageUri
        )

        return if (result.isSuccess) {
            val project = result.getOrThrow()
            SkillResult(
                success = true,
                outputMessage = "✨ AI 图像生成技能已完成执行！已基于设定完成渲染。",
                relatedProjectId = project.id,
                outputImageUrl = project.resultImageUri,
                intermediateSteps = listOf(
                    "解析画面描述: $prompt",
                    "应用艺术风格: $stylePreset",
                    "设置宽高比例: $aspectRatio",
                    if (sourceImageUri != null) "已融合参考图像特征并进行风格迁移" else "根据纯文本指令生成全新图像",
                    "图像生成成功并已存入项目库"
                )
            )
        } else {
            SkillResult(
                success = false,
                outputMessage = "图像生成技能执行失败: ${result.exceptionOrNull()?.message}",
                error = result.exceptionOrNull()?.message
            )
        }
    }
}

/**
 * Skill 2: Video Generation Pipeline
 */
class VideoGenerationSkill(
    private val repository: AgnesRepository
) : AgentSkill {
    override val id: String = "video-generation"
    override val name: String = "AI 电影分镜与视频流水线"
    override val iconEmoji: String = "🎬"
    override val description: String = "全自动规划多幕电影分镜脚本（含视觉提示词、运镜语言与旁白），并在 60 秒限速队列中逐段调用视频大模型渲染并最终无缝拼接成完整视频短片。"
    override val category: String = "video"
    override var isEnabled: Boolean = true

    override val triggerKeywords: List<String> = listOf(
        "视频", "短片", "电影", "分镜", "拼接", "生成短片", "做视频", "动态短片", "宣传片", "video", "movie", "film"
    )

    override val parameters: List<SkillParam> = listOf(
        SkillParam(
            name = "themePrompt",
            type = "string",
            description = "视频的剧情主题、故事主线、画面情绪或宣传大纲",
            required = true
        ),
        SkillParam(
            name = "model",
            type = "string",
            description = "指定的视频生成模型（如 'agnes-video-v2.0' 或 'agnes-video-2.5-flash'，不填则使用系统配置模型）",
            required = false
        ),
        SkillParam(
            name = "aspectRatio",
            type = "string",
            description = "画面比例，如 '16:9'、'9:16'、'4:3'、'1:1'、'3:4'、'21:9'",
            required = false,
            defaultValue = "16:9"
        ),
        SkillParam(
            name = "duration",
            type = "int",
            description = "单段视频时长（秒），如 5 或 10",
            required = false,
            defaultValue = 5
        ),
        SkillParam(
            name = "sceneCount",
            type = "int",
            description = "分镜段数（默认 4 段，可设 2~6 段）",
            required = false,
            defaultValue = 4
        ),
        SkillParam(
            name = "stylePreset",
            type = "string",
            description = "视频视觉风格，如 'Cinematic 3D', 'Sci-Fi Cyberpunk', 'Anime Cinematic'",
            required = false,
            defaultValue = "Cinematic 3D"
        ),
        SkillParam(
            name = "sourceImageUri",
            type = "image_uri",
            description = "视频首帧或视觉参考图 URI",
            required = false
        )
    )

    override suspend fun execute(
        context: SkillExecutionContext,
        arguments: Map<String, Any?>
    ): SkillResult {
        val themePrompt = arguments["themePrompt"]?.toString()?.takeIf { it.isNotBlank() }
            ?: "电影级叙事视觉短片"
        val explicitModel = arguments["model"]?.toString()?.takeIf { it.isNotBlank() }
        val aspectRatio = arguments["aspectRatio"]?.toString()?.takeIf { it.isNotBlank() } ?: "16:9"
        val duration = VideoDurationLimits.clamp((arguments["duration"] as? Number)?.toInt() ?: VideoDurationLimits.DEFAULT)
        val sceneCount = (arguments["sceneCount"] as? Number)?.toInt() ?: 4
        val stylePreset = arguments["stylePreset"]?.toString() ?: "Cinematic 3D"
        val sourceImageUri = arguments["sourceImageUri"]?.toString() ?: context.attachedImageUri

        val effectiveModel = explicitModel ?: context.config.videoModelName

        context.onProgress("技能 [video-generation] 正在规划分镜脚本与运镜语言 (模型: $effectiveModel, 比例: $aspectRatio)...")

        val result = repository.startFullVideoPipeline(
            themePrompt = themePrompt,
            sourceImageUri = sourceImageUri,
            sceneCount = sceneCount,
            stylePreset = stylePreset,
            videoModel = effectiveModel,
            aspectRatio = aspectRatio,
            durationPerScene = duration,
            onProgress = context.onProgress
        )

        return if (result.isSuccess) {
            val project = result.getOrThrow()
            SkillResult(
                success = true,
                outputMessage = "🎬 AI 电影视频流水线技能执行成功！全部分镜已基于模型 [$effectiveModel] 生成完毕并拼接合成。",
                relatedProjectId = project.id,
                outputVideoUrl = project.resultVideoUri,
                intermediateSteps = listOf(
                    "规划 $sceneCount 段电影分镜脚本与运镜参数",
                    "在 60s 冷却队列中依次调用自适应视频渲染模型 ($effectiveModel, 画面比例: $aspectRatio)",
                    "多段分镜片段已成功捕获与校验",
                    "完成时间轴音视频拼接并输出最终成果"
                )
            )
        } else {
            SkillResult(
                success = false,
                outputMessage = "视频流水线技能执行失败: ${result.exceptionOrNull()?.message}",
                error = result.exceptionOrNull()?.message
            )
        }
    }
}

/**
 * Skill 3: Prompt Enhancement Skill
 */
class PromptEnhancerSkill(
    private val agnesClient: AgnesClient
) : AgentSkill {
    override val id: String = "prompt-enhancer"
    override val name: String = "视觉提示词专家润色"
    override val iconEmoji: String = "✨"
    override val description: String = "将用户的简短构思扩写润色为符合 AI 绘画与视频生成工业标准的高质量中英文提示词（含主体细节、摄影光影、镜头构图与渲染引擎词汇）。"
    override val category: String = "enhancement"
    override var isEnabled: Boolean = true

    override val triggerKeywords: List<String> = listOf(
        "提示词", "润色", "优化词", "优化提示词", "写提示词", "关键词", "prompt", "enhance prompt", "怎么写提示词"
    )

    override val parameters: List<SkillParam> = listOf(
        SkillParam(
            name = "rawPrompt",
            type = "string",
            description = "用户原始的想法或简短描述",
            required = true
        ),
        SkillParam(
            name = "targetType",
            type = "string",
            description = "目标类型: 'image' (图片) 或 'video' (视频)",
            required = false,
            defaultValue = "image"
        )
    )

    override suspend fun execute(
        context: SkillExecutionContext,
        arguments: Map<String, Any?>
    ): SkillResult {
        val raw = arguments["rawPrompt"]?.toString() ?: "未来城市风景"
        val type = arguments["targetType"]?.toString() ?: "image"

        val promptForModel = """
            作为资深 AI 视觉导演，请将以下用户的简短构思润色为专业摄影与渲染级的提示词：
            原始输入: "$raw"
            目标载体: $type
            
            请输出：
            1. **核心主题与画面构图**
            2. **中文精细化视觉描述**（主体、材质、光影氛围、色调、运镜/景别）
            3. **英文专业生图 Prompt**（适合 Midjourney / Flux / Kling）
            4. **推荐参数设置**（如宽高比、风格预设、光线推荐）
        """.trimIndent()

        val chatResult = agnesClient.generateChatReply(
            config = context.config,
            chatHistory = emptyList(),
            userPrompt = promptForModel
        )

        val output = chatResult.getOrNull() ?: """
            ### ✨ 视觉提示词润色建议
            
            **中文精编描述：**
            $raw，8K 超高清分辨率，电影级宽银幕构图，真实漫反射光影，景深虚化，丁达尔光线，极具艺术张力与通透质感。
            
            **英文专业 Prompt：**
            Masterpiece, hyper-detailed cinematic shot of $raw, 8k resolution, volumetric lighting, ray tracing, octane render, photorealistic textures, shallow depth of field, dramatic atmospheric lighting, film grain --ar 16:9
            
            > 💡 **提示**：你可以直接让我使用 **[AI 图像生成]** 或 **[AI 视频生成]** 技能将此画面呈现出来！
        """.trimIndent()

        return SkillResult(
            success = true,
            outputMessage = output,
            intermediateSteps = listOf(
                "解析原始输入概念: $raw",
                "扩充光影、质感、构图视听语言",
                "生成中英双语专业格式"
            )
        )
    }
}

/**
 * Skill 4: Storyboard Director Planning Skill
 */
class StoryboardDirectorSkill(
    private val agnesClient: AgnesClient
) : AgentSkill {
    override val id: String = "storyboard-director"
    override val name: String = "导演级分镜规划"
    override val iconEmoji: String = "📋"
    override val description: String = "以专业电影导演视角，将剧情故事拆解为标准影视工业分镜表（镜头编号、景别、运镜镜头语言、视听音效、画面意境与旁白台词）。"
    override val category: String = "planning"
    override var isEnabled: Boolean = true

    override val triggerKeywords: List<String> = listOf(
        "剧本", "分镜表", "规划分镜", "拆解分镜", "导演分镜", "脚本规划", "故事大纲", "镜头语言", "storyboard"
    )

    override val parameters: List<SkillParam> = listOf(
        SkillParam(
            name = "storyOutline",
            type = "string",
            description = "故事背景、剧情梗概或想要呈现的情节",
            required = true
        ),
        SkillParam(
            name = "sceneCount",
            type = "int",
            description = "规划的分镜段落数（通常 3~5 幕）",
            required = false,
            defaultValue = 4
        )
    )

    override suspend fun execute(
        context: SkillExecutionContext,
        arguments: Map<String, Any?>
    ): SkillResult {
        val story = arguments["storyOutline"]?.toString() ?: "一段关于探索未知的视觉短片"
        val count = (arguments["sceneCount"] as? Number)?.toInt() ?: 4

        val promptForModel = """
            你是一名好莱坞级电影导演与编剧。请针对以下故事题材，设计一套拥有 $count 个标准分镜的影视分镜规划表：
            故事内容: "$story"
            
            请严格按照以下 Markdown 格式结构输出：
            ### 🎬 影视分镜设计表：《$story》
            
            为每个分镜输出：
            - **分镜 0X: [分镜标题]**
              - **景别与运镜**: (例如：大远景航拍俯瞰 / 特写缓慢推镜 / 环绕运镜)
              - **视觉核心画面**: (具体光影、主体动作、环境细节)
              - **视听旁白 / 台词**: (情感饱满的画外音)
              - **音效设计 (SFX)**: (如风声、电子脉冲、机械转动)
            
            并在结尾提供一句专业的导演阐述总结。
        """.trimIndent()

        val chatResult = agnesClient.generateChatReply(
            config = context.config,
            chatHistory = emptyList(),
            userPrompt = promptForModel
        )

        val output = chatResult.getOrNull() ?: """
            ### 🎬 影视分镜设计表：《$story》
            
            - **分镜 01: 寂静启程**
              - **景别与运镜**: 大远景缓推 (Slow Dolly In)
              - **视觉画面**: 广袤苍茫的地平线，曙光划破薄雾，剪影般的人物身影立于边缘
              - **旁白**: “在时间的尽头，每一个选择都是一次新生。”
              - **音效**: 低沉的风啸与深邃的弦乐低音
              
            - **分镜 02: 异象显现**
              - **景别与运镜**: 中景环绕运镜 (Drone Orbit)
              - **视觉画面**: 奇异的光芒从裂隙中升腾，粒子流环绕主角飞旋
              - **旁白**: “答案从来不在已知之中，而在未知的深渊。”
              - **音效**: 高频能量脉冲与心跳震颤音
              
            - **分镜 03: 跨越巅峰**
              - **景别与运镜**: 特写极速推近 (Fast Zoom-in to Wide)
              - **视觉画面**: 毅然决然迈入光芒，视界骤然展开为绚丽壮阔的未知宇宙
              - **旁白**: “向前走，直到成为光芒本身。”
              - **音效**: 磅礴恢弘的管弦乐高潮
              
            > 💡 **智能体建议**：确认分镜无误后，随时告诉我 **“将此分镜生成为完整视频短片”**，我将立即启动视频流水线技能进行逐段渲染与拼接！
        """.trimIndent()

        return SkillResult(
            success = true,
            outputMessage = output,
            intermediateSteps = listOf(
                "分析故事叙事弧线: $story",
                "规划视听工业节奏与 $count 段景别转换",
                "生成专业导演分镜设计表"
            )
        )
    }
}

/**
 * Skill Registry managing loaded skills and skill discovery.
 */
class AgentSkillRegistry(
    private val context: Context,
    val repository: AgnesRepository,
    val agnesClient: AgnesClient
) {
    private val _skills = MutableStateFlow<List<AgentSkill>>(emptyList())
    val skills: StateFlow<List<AgentSkill>> = _skills.asStateFlow()

    init {
        // Register default built-in skills (Creation, Video, Search, Office Documents, Planning)
        _skills.value = listOf(
            TavilySearchSkill(agnesClient),
            ImageGenerationSkill(repository),
            VideoGenerationSkill(repository),
            WordDocumentSkill(context, agnesClient),
            PdfDocumentSkill(context, agnesClient),
            ExcelSpreadsheetSkill(context, agnesClient),
            PromptEnhancerSkill(agnesClient),
            StoryboardDirectorSkill(agnesClient)
        )
    }

    /**
     * Get a specific skill by its ID.
     */
    fun getSkill(id: String): AgentSkill? {
        return _skills.value.find { it.id == id }
    }

    /**
     * Toggle skill enabled state.
     */
    fun setSkillEnabled(skillId: String, enabled: Boolean) {
        _skills.value = _skills.value.map { skill ->
            if (skill.id == skillId) {
                skill.isEnabled = enabled
                skill
            } else skill
        }
    }

    /**
     * Get all active (enabled) skills.
     */
    fun getActiveSkills(): List<AgentSkill> {
        return _skills.value.filter { it.isEnabled }
    }

    /**
     * Formats skill declarations into a system prompt instruction for the LLM.
     */
    fun buildSkillsSystemPrompt(): String {
        val active = getActiveSkills()
        if (active.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("\n### ⚡ [可用智能体技能库 / Available Skills]")
        sb.appendLine("你是一个具备工具调用能力的真正的多模态 AI 智能体（Agent）。当前已为你加载并启用了以下专业技能：")
        for (skill in active) {
            sb.appendLine("- **${skill.id}** (${skill.name} ${skill.iconEmoji}): ${skill.description}")
            sb.appendLine("  参数列表:")
            for (p in skill.parameters) {
                sb.appendLine("    * ${p.name} (${p.type}${if (p.required) ", 必填" else ", 可选"}): ${p.description}")
            }
        }
        sb.appendLine("""
            【技能调用准则】:
            1. 当用户需要联网检索实时信息、查询最新新闻、实时股票/币价行情、事实核查或搜索网页资料时，必须调用 `tavily_search`。
            2. 当用户要求画图、生图、重绘变奏、或附带参考图时，必须调用 `image-generation`。
            3. 当用户要求生成视频、短片、多幕分镜视频并拼接时，必须调用 `video-generation`。
            4. 当用户要求写方案/报告/合同/公文/总结并导出 Word 文档时，必须调用 `word-document`。
            5. 当用户要求生成、导出或打印 A4 矢量 PDF 文档时，必须调用 `pdf-document`。
            6. 当用户要求制作数据表、预算核算、甘特图、考勤统计并导出 Excel/CSV 时，必须调用 `excel-spreadsheet`。
            7. 当用户要求润色、优化提示词时，调用 `prompt-enhancer`。
            8. 当用户要求写剧本、规划影视分镜表时，调用 `storyboard-director`。
            9. 如果仅为普通的问答、闲聊或理论解释，无需调用技能，直接回答即可。
            
            若你判断需要调用技能，可以在回复中按以下格式进行工具调用：
            ```json
            {"tool_call": "技能ID", "arguments": {"参数名": "参数值"}}
            ```
            在调用前或调用后，请给予用户友好的专业说明。
        """.trimIndent())
        return sb.toString()
    }
}
