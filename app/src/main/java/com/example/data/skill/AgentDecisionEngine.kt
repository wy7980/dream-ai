package com.example.data.skill

import com.example.data.api.AgnesClient
import com.example.data.model.AgnesApiConfig
import com.example.data.model.ChatIntentMode
import com.example.data.model.ChatMessage
import org.json.JSONObject

sealed class AgentDecision {
    data class InvokeSkill(
        val skill: AgentSkill,
        val arguments: Map<String, Any?>,
        val preThoughtText: String
    ) : AgentDecision()

    data class DirectChatReply(
        val replyText: String
    ) : AgentDecision()
}

/**
 * Intelligent decision-making engine that analyzes context and routes to skills or natural chat.
 */
class AgentDecisionEngine(
    private val skillRegistry: AgentSkillRegistry,
    private val agnesClient: AgnesClient
) {
    /**
     * Determines whether to invoke a skill or reply directly with text.
     */
    suspend fun decide(
        config: AgnesApiConfig,
        userPrompt: String,
        attachedImageUri: String?,
        chatHistory: List<ChatMessage>,
        mode: ChatIntentMode
    ): AgentDecision {
        val activeSkills = skillRegistry.getActiveSkills()
        val lower = userPrompt.lowercase().trim()

        // 1. Check explicit Mode overrides
        if (mode == ChatIntentMode.IMAGE_GEN) {
            val imageSkill = activeSkills.find { it.id == "image-generation" }
            if (imageSkill != null) {
                return AgentDecision.InvokeSkill(
                    skill = imageSkill,
                    arguments = mapOf(
                        "prompt" to if (userPrompt.isNotBlank()) userPrompt else "基于参考图的艺术变奏重绘",
                        "sourceImageUri" to attachedImageUri,
                        "stylePreset" to "Cinematic 3D",
                        "aspectRatio" to "16:9"
                    ),
                    preThoughtText = "🧠 [智能体思考] 用户指定了图像生成模式。已加载并调度技能 `[image-generation]`。"
                )
            }
        }

        if (mode == ChatIntentMode.VIDEO_GEN) {
            val videoSkill = activeSkills.find { it.id == "video-generation" }
            if (videoSkill != null) {
                return AgentDecision.InvokeSkill(
                    skill = videoSkill,
                    arguments = mapOf(
                        "themePrompt" to if (userPrompt.isNotBlank()) userPrompt else "多幕史诗电影视频短片",
                        "sourceImageUri" to attachedImageUri,
                        "sceneCount" to 4,
                        "stylePreset" to "Cinematic 3D"
                    ),
                    preThoughtText = "🧠 [智能体思考] 用户指定了视频生成模式。已加载并调度技能 `[video-generation]` 规划分镜并逐段渲染合成。"
                )
            }
        }

        if (mode == ChatIntentMode.CHAT) {
            // Strict chat mode, no skill calls
            val replyResult = agnesClient.generateChatReply(config, chatHistory, userPrompt)
            return AgentDecision.DirectChatReply(
                replyResult.getOrNull() ?: "你好！我是 Dream AI 智能体，有什么我可以帮你的？"
            )
        }

        // 2. AUTO Mode: Try asking LLM with injected Skills instructions
        if (config.apiKey.isNotBlank()) {
            val skillsSystemPrompt = skillRegistry.buildSkillsSystemPrompt()
            val enhancedHistory = chatHistory.toMutableList()
            // In AUTO mode, ask the LLM if a skill should be triggered
            val agentPrompt = """
                $userPrompt
                ${if (attachedImageUri != null) "[注: 用户上传了一张参考图片]" else ""}
            """.trimIndent()

            val llmResponse = agnesClient.generateChatReply(
                config = config,
                chatHistory = enhancedHistory,
                userPrompt = "$skillsSystemPrompt\n\n用户消息: $agentPrompt"
            )

            val responseText = llmResponse.getOrNull() ?: ""

            // Check if response contains a tool call JSON
            val toolCall = extractToolCall(responseText)
            if (toolCall != null) {
                val skill = activeSkills.find { it.id == toolCall.skillId }
                if (skill != null) {
                    val cleanText = responseText.replace(Regex("```json[\\s\\S]*?```"), "").trim()
                    return AgentDecision.InvokeSkill(
                        skill = skill,
                        arguments = toolCall.arguments,
                        preThoughtText = if (cleanText.isNotBlank()) cleanText else "🧠 [智能体决策] 识别到创作意图，正调用技能 `[${skill.name}]` 执行任务..."
                    )
                }
            } else if (responseText.isNotBlank() && !isHeuristicMatch(userPrompt, attachedImageUri)) {
                // If model gave a thoughtful reply and no obvious skill keyword triggered
                return AgentDecision.DirectChatReply(responseText)
            }
        }

        // 3. Heuristic Intent Pattern Recognition (Robust fallback for instant response or offline)
        val imageSkill = activeSkills.find { it.id == "image-generation" }
        val videoSkill = activeSkills.find { it.id == "video-generation" }
        val wordSkill = activeSkills.find { it.id == "word-document" }
        val pdfSkill = activeSkills.find { it.id == "pdf-document" }
        val excelSkill = activeSkills.find { it.id == "excel-spreadsheet" }
        val promptSkill = activeSkills.find { it.id == "prompt-enhancer" }
        val directorSkill = activeSkills.find { it.id == "storyboard-director" }

        // Excel table intent check
        if (excelSkill != null && (
                    lower.contains("excel") || lower.contains("表格") ||
                    lower.contains("做个表") || lower.contains("预算表") ||
                    lower.contains("统计表") || lower.contains("考勤表") ||
                    lower.contains("销售表") || lower.contains("csv") ||
                    lower.contains("xlsx") || lower.contains("甘特图") ||
                    (lower.contains("数据") && lower.contains("表"))
                )) {
            return AgentDecision.InvokeSkill(
                skill = excelSkill,
                arguments = mapOf(
                    "sheetTitle" to userPrompt,
                    "dataScope" to "业务明细数据"
                ),
                preThoughtText = "🧠 [智能体思考] 检测到电子表格与数据统计需求，已调度 `[${excelSkill.name}]` 技能构建多维数据表并导出标准 Excel 文件。"
            )
        }

        // PDF document intent check
        if (pdfSkill != null && (
                    lower.contains("pdf") || lower.contains("导出pdf") ||
                    lower.contains("生成pdf") || lower.contains("转成pdf") ||
                    lower.contains("打印文档")
                )) {
            return AgentDecision.InvokeSkill(
                skill = pdfSkill,
                arguments = mapOf(
                    "title" to userPrompt,
                    "content" to userPrompt
                ),
                preThoughtText = "🧠 [智能体思考] 检测到矢量文档编译与打印诉求，已调度 `[${pdfSkill.name}]` 技能排版并编译标准 A4 PDF 文档。"
            )
        }

        // Word document intent check
        if (wordSkill != null && (
                    lower.contains("word") || lower.contains("doc") ||
                    lower.contains("docx") || lower.contains("公文") ||
                    lower.contains("合同") || lower.contains("协议") ||
                    lower.contains("企划案") || lower.contains("策划案") ||
                    lower.contains("会议纪要") || lower.contains("工作汇报") ||
                    (lower.contains("写") && (lower.contains("报告") || lower.contains("方案")))
                )) {
            return AgentDecision.InvokeSkill(
                skill = wordSkill,
                arguments = mapOf(
                    "topic" to userPrompt,
                    "docType" to if (lower.contains("合同")) "商务合同" else if (lower.contains("企划")) "商业企划案" else "专业方案报告"
                ),
                preThoughtText = "🧠 [智能体思考] 检测到结构化文档撰写诉求，已调度 `[${wordSkill.name}]` 技能设计公文目录架构并生成 Word 文档。"
            )
        }

        // Video intent check
        if (videoSkill != null && (
                    lower.contains("视频") || lower.contains("短片") ||
                    lower.contains("电影") || lower.contains("分镜拼接") ||
                    lower.contains("video") || lower.contains("movie") ||
                    (lower.contains("生成") && lower.contains("片"))
                )) {
            return AgentDecision.InvokeSkill(
                skill = videoSkill,
                arguments = mapOf(
                    "themePrompt" to userPrompt,
                    "sourceImageUri" to attachedImageUri,
                    "sceneCount" to 4,
                    "stylePreset" to "Cinematic 3D"
                ),
                preThoughtText = "🧠 [智能体思考] 检测到视频创作指令，已自主调度 `[${videoSkill.name}]` 技能规划视听剧本并开启渲染合成流水线。"
            )
        }

        // Storyboard director check
        if (directorSkill != null && (
                    lower.contains("分镜表") || lower.contains("规划分镜") ||
                    lower.contains("导演分镜") || lower.contains("剧本拆解") ||
                    lower.contains("故事大纲")
                )) {
            return AgentDecision.InvokeSkill(
                skill = directorSkill,
                arguments = mapOf(
                    "storyOutline" to userPrompt,
                    "sceneCount" to 4
                ),
                preThoughtText = "🧠 [智能体思考] 已加载 `[${directorSkill.name}]` 技能，正在为你进行专业电影级分镜拆解与视听镜头语言设计。"
            )
        }

        // Prompt enhancer check
        if (promptSkill != null && (
                    lower.contains("润色") || lower.contains("优化提示词") ||
                    lower.contains("优化词") || lower.contains("写提示词") ||
                    lower.contains("提示词") && (lower.contains("帮我") || lower.contains("怎么写"))
                )) {
            return AgentDecision.InvokeSkill(
                skill = promptSkill,
                arguments = mapOf(
                    "rawPrompt" to userPrompt,
                    "targetType" to if (lower.contains("视频")) "video" else "image"
                ),
                preThoughtText = "🧠 [智能体思考] 已加载 `[${promptSkill.name}]` 技能，正在为你扩展中英文摄影构图与高级渲染材质关键词。"
            )
        }

        // Image intent check
        if (imageSkill != null && (
                    attachedImageUri != null ||
                    lower.contains("画") || lower.contains("生图") ||
                    lower.contains("重绘") || lower.contains("插画") ||
                    lower.contains("壁纸") || lower.contains("原画") ||
                    lower.contains("图片") || lower.contains("image") || lower.contains("draw")
                )) {
            return AgentDecision.InvokeSkill(
                skill = imageSkill,
                arguments = mapOf(
                    "prompt" to if (userPrompt.isNotBlank()) userPrompt else "基于参考图的艺术变奏重绘",
                    "sourceImageUri" to attachedImageUri,
                    "stylePreset" to "Cinematic 3D",
                    "aspectRatio" to "16:9"
                ),
                preThoughtText = "🧠 [智能体思考] 检测到图像生成/变奏意图，已加载并调用 `[${imageSkill.name}]` 技能执行高清渲染。"
            )
        }

        // 4. Default: Conversational reply
        val fallbackReply = agnesClient.generateChatReply(config, chatHistory, userPrompt)
        return AgentDecision.DirectChatReply(
            fallbackReply.getOrNull() ?: "你好！我是 Dream AI 智能体。我已装载了包括【AI 图像生成】、【电影视频流水线】、【Word 文档排版】、【PDF 矢量编译】与【Excel 数据建模】等多项专业技能。你可以告诉我你的需求，我将自主调度对应技能为你实现！"
        )
    }

    private fun isHeuristicMatch(prompt: String, attachedImageUri: String?): Boolean {
        if (attachedImageUri != null) return true
        val lower = prompt.lowercase()
        return lower.contains("画") || lower.contains("生图") ||
                lower.contains("视频") || lower.contains("短片") ||
                lower.contains("word") || lower.contains("pdf") ||
                lower.contains("excel") || lower.contains("表格") ||
                lower.contains("报告") || lower.contains("润色") || lower.contains("分镜")
    }

    private data class ParsedToolCall(val skillId: String, val arguments: Map<String, Any?>)

    private fun extractToolCall(text: String): ParsedToolCall? {
        try {
            val jsonPattern = Regex("```json\\s*([\\s\\S]*?)\\s*```")
            val match = jsonPattern.find(text)
            val jsonStr = match?.groupValues?.get(1) ?: if (text.trim().startsWith("{") && text.trim().endsWith("}")) text.trim() else null
            if (jsonStr != null) {
                val obj = JSONObject(jsonStr)
                if (obj.has("tool_call")) {
                    val skillId = obj.getString("tool_call")
                    val argsObj = obj.optJSONObject("arguments") ?: JSONObject()
                    val args = mutableMapOf<String, Any?>()
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        args[k] = argsObj.get(k)
                    }
                    return ParsedToolCall(skillId, args)
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
