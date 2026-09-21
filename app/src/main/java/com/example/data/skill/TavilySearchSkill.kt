package com.example.data.skill

import com.example.data.api.AgnesClient
import com.example.data.model.TavilySearchResultItem
import kotlinx.coroutines.delay

/**
 * Tavily AI Real-Time Web Search Skill.
 * Enables the Agent to query the live internet for up-to-date facts, news, and research data.
 */
class TavilySearchSkill(
    private val agnesClient: AgnesClient
) : AgentSkill {

    override val id: String = "tavily_search"
    override val name: String = "Tavily 实时联网搜索"
    override val iconEmoji: String = "🌐"
    override val description: String =
        "基于 Tavily AI 搜索引擎进行全网实时网页搜索与信息检索，获取最新权威新闻、事实数据、行业情报，并输出带引用来源与结构化摘要的权威答案。"
    override val category: String = "search"
    override var isEnabled: Boolean = true

    override val triggerKeywords: List<String> = listOf(
        "搜索", "搜一下", "查一下", "查查", "联网", "实时", "最新消息", "最新新闻",
        "最新进展", "检索", "资讯", "行情", "tavily", "search", "web_search", "google", "bing", "查找资料"
    )

    override val parameters: List<SkillParam> = listOf(
        SkillParam(
            name = "query",
            type = "string",
            description = "搜索关键词、问题或资讯主题（例如：'2026年最新AI大模型进展', '苹果春季发布会最新动态'）",
            required = true
        ),
        SkillParam(
            name = "search_depth",
            type = "string",
            description = "搜索深度：'basic'（极速精简模式，响应快）或 'advanced'（深度研究模式，爬取更多细节与长文）",
            required = false,
            defaultValue = "basic"
        ),
        SkillParam(
            name = "max_results",
            type = "int",
            description = "最大返回网页结果数量（推荐 3-8 条）",
            required = false,
            defaultValue = 5
        ),
        SkillParam(
            name = "include_answer",
            type = "boolean",
            description = "是否由 Tavily 生成综合智能答案摘要",
            required = false,
            defaultValue = true
        )
    )

    override suspend fun execute(
        context: SkillExecutionContext,
        arguments: Map<String, Any?>
    ): SkillResult {
        val query = (arguments["query"] as? String)?.trim()
            ?: (arguments["q"] as? String)?.trim()
            ?: "最新科技与人工智能资讯"

        val searchDepth = (arguments["search_depth"] as? String)?.trim()?.lowercase()
            ?.takeIf { it == "advanced" } ?: "basic"

        val maxResults = when (val mr = arguments["max_results"]) {
            is Number -> mr.toInt().coerceIn(1, 10)
            is String -> mr.toIntOrNull()?.coerceIn(1, 10) ?: 5
            else -> 5
        }

        val includeAnswer = when (val ia = arguments["include_answer"]) {
            is Boolean -> ia
            is String -> ia.toBooleanStrictOrNull() ?: true
            else -> true
        }

        val steps = mutableListOf<String>()
        steps.add("收到联网检索指令，检索目标: 「$query」")
        context.onProgress("🌐 [tavily_search] 正在初始化 Tavily 实时搜索引擎...")
        delay(150)

        // Determine Tavily API Key
        val apiKey = context.config.tavilyApiKey.trim().ifBlank {
            (arguments["apiKey"] as? String)?.trim() ?: ""
        }

        if (apiKey.isBlank()) {
            steps.add("⚠️ 未检测到配置的 Tavily API Key")
            return SkillResult(
                success = false,
                outputMessage = """
                    ### ⚠️ 未配置 Tavily 搜索密钥 (API Key)
                    
                    **Tavily 实时联网搜索技能** 需要有效的 Tavily API Key 才能连接互联网检索最新数据。
                    
                    ---
                    #### 🚀 快速配置步骤：
                    1. 前往 **[Tavily 官网 (https://tavily.com)](https://tavily.com)** 免费注册并复制你的 API Key（以 `tvly-` 开头，每月享有免费搜索配额）；
                    2. 点击应用右下角 **【设置】->【🌐 Tavily 联网搜索】** 或在聊天界面顶部的 **【技能库】** 中找到 Tavily 技能，填入密钥并保存；
                    3. 保存后重新向智能体提问，即可自动联网检索实时全网资讯！
                """.trimIndent(),
                intermediateSteps = steps,
                error = "TAVILY_API_KEY_NOT_CONFIGURED"
            )
        }

        steps.add("已加载 Tavily 认证密钥，搜索模式: ${if (searchDepth == "advanced") "深度研究 (Advanced)" else "极速标准 (Basic)"}，最大结果: $maxResults")
        context.onProgress("🔍 正在检索实时全网数据并提取权威信息源...")

        val searchResult = agnesClient.performTavilySearch(
            apiKey = apiKey,
            query = query,
            searchDepth = searchDepth,
            maxResults = maxResults,
            includeAnswer = includeAnswer
        )

        if (searchResult.isFailure) {
            val error = searchResult.exceptionOrNull()?.message ?: "未知网络错误"
            steps.add("❌ Tavily 搜索请求异常: $error")
            return SkillResult(
                success = false,
                outputMessage = """
                    ### ❌ Tavily 联网搜索失败
                    
                    在调用 Tavily 实时搜索服务时遇到了异常：
                    > **错误详情**: $error
                    
                    💡 **排查建议**:
                    - 请检查你的 Tavily API Key 是否有效或配额是否充足；
                    - 请检查设备网络连接是否通畅；
                    - 你可在【设置】中重新配置或点击【测试连接】进行诊断。
                """.trimIndent(),
                intermediateSteps = steps,
                error = error
            )
        }

        val data = searchResult.getOrThrow()
        steps.add("成功检索到 ${data.results.size} 条高质量实时网页来源与分析数据")
        context.onProgress("📑 正在整合权威来源并生成结构化检索报告...")
        delay(100)
        steps.add("生成结构化联网研究报告完成")

        val formattedOutput = formatTavilyResponse(query, data, searchDepth)

        return SkillResult(
            success = true,
            outputMessage = formattedOutput,
            intermediateSteps = steps
        )
    }

    private fun formatTavilyResponse(
        query: String,
        data: com.example.data.model.TavilySearchResponse,
        searchDepth: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("### 🌐 Tavily 实时联网检索报告")
        sb.appendLine("> 🔍 **检索关键词**: `$query` ｜ **模式**: `${if (searchDepth == "advanced") "深度研究 (Advanced)" else "极速 (Basic)"}` ｜ **来源数**: `${data.results.size}`\n")

        // 1. Executive Summary Answer if provided by Tavily
        if (!data.answer.isNullOrBlank()) {
            sb.appendLine("#### 🤖 智能综合摘要 (AI Executive Summary)")
            sb.appendLine(data.answer.trim())
            sb.appendLine()
        }

        // 2. Structured Results List with Citations
        if (data.results.isNotEmpty()) {
            sb.appendLine("#### 📚 权威参考来源与实时资讯 (Sources & Highlights)")
            data.results.forEachIndexed { index, item ->
                val num = index + 1
                val scorePercent = if (item.score > 0.0) " (${(item.score * 100).toInt()}% 相关度)" else ""
                val dateStr = if (!item.publishedDate.isNullOrBlank()) " • *${item.publishedDate}*" else ""
                
                sb.appendLine("**[$num] [${item.title.ifBlank { "相关资讯" }}](${item.url})**$scorePercent$dateStr")
                if (item.content.isNotBlank()) {
                    val cleanSnippet = item.content.trim().replace("\n", " ")
                    sb.appendLine("> ${cleanSnippet.take(280)}${if (cleanSnippet.length > 280) "..." else ""}")
                }
                sb.appendLine()
            }
        } else {
            sb.appendLine("*未找到匹配的具体网页来源，建议尝试更换搜索关键词。*")
        }

        sb.appendLine("---")
        sb.appendLine("💡 *数据由 Tavily AI 实时搜索引擎提供支持。智能体可基于以上实时事实数据继续深入分析。*")

        return sb.toString().trim()
    }
}
