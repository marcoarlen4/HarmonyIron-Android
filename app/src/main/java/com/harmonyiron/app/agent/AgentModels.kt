package com.harmonyiron.app.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val tools: List<ToolDefinitionDto>? = null,
    val temperature: Double = 0.3
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDto>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null
)

@Serializable
data class ToolDefinitionDto(
    val type: String = "function",
    val function: FunctionSpecDto
)

@Serializable
data class FunctionSpecDto(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

@Serializable
data class ToolCallDto(
    val id: String,
    val type: String = "function",
    val function: FunctionCallDto
)

@Serializable
data class FunctionCallDto(
    val name: String,
    val arguments: String
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChoiceDto> = emptyList()
)

@Serializable
data class ChoiceDto(
    val message: ChatMessageDto
)

/**
 * 计划修改提案（由 AI 通过 propose_plan_diff 工具输出，经由用户确认或选择性勾选后落盘）。
 */
@Serializable
data class PlanDiffProposal(
    val summary: String,
    val changes: List<DiffChangeItem> = emptyList()
)

@Serializable
data class DiffChangeItem(
    val id: String,
    val actionType: String, // REPLACE_EXERCISE / UPDATE_PARAMS / ADD_EXERCISE / REMOVE_EXERCISE
    val planDayId: Long,
    val targetItemId: Long? = null,
    val newExerciseId: Long? = null,
    val newSets: Int? = null,
    val newReps: Int? = null,
    val newRestSeconds: Int? = null,
    val title: String,
    val beforeDesc: String,
    val afterDesc: String,
    val reason: String
)

/**
 * UI 展示用的教练回复结果。
 */
data class CoachAgentResponse(
    val analysisReport: String,
    val diffProposal: PlanDiffProposal? = null,
    val executedTools: List<String> = emptyList()
)
