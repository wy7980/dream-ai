package com.example.data.skill

import com.example.data.model.AgnesApiConfig

/**
 * Parameter definition for a Skill.
 */
data class SkillParam(
    val name: String,
    val type: String, // "string", "int", "boolean", "image_uri"
    val description: String,
    val required: Boolean = false,
    val defaultValue: Any? = null
)

/**
 * Execution context provided when a skill is triggered.
 */
data class SkillExecutionContext(
    val config: AgnesApiConfig,
    val attachedImageUri: String? = null,
    val onProgress: (String) -> Unit = {}
)

/**
 * Result returned after a skill completes execution.
 */
data class SkillResult(
    val success: Boolean,
    val outputMessage: String,
    val relatedProjectId: String? = null,
    val outputImageUrl: String? = null,
    val outputVideoUrl: String? = null,
    val intermediateSteps: List<String> = emptyList(),
    val error: String? = null
)

/**
 * Metadata & execution contract of an Agent Skill.
 */
interface AgentSkill {
    val id: String
    val name: String
    val iconEmoji: String
    val description: String
    val category: String // "creation", "video", "enhancement", "planning"
    val parameters: List<SkillParam>
    var isEnabled: Boolean
    val triggerKeywords: List<String>

    suspend fun execute(
        context: SkillExecutionContext,
        arguments: Map<String, Any?>
    ): SkillResult
}

/**
 * Information describing a Skill Invocation performed by the Agent.
 */
data class SkillInvocationRecord(
    val skillId: String,
    val skillName: String,
    val iconEmoji: String,
    val arguments: Map<String, Any?>,
    val status: InvocationStatus,
    val statusMessage: String = "",
    val executionTimeMs: Long = 0L,
    val resultSummary: String? = null,
    val relatedProjectId: String? = null
)

enum class InvocationStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILED
}
