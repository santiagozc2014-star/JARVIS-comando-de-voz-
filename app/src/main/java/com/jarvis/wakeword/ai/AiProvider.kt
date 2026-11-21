package com.jarvis.wakeword.ai

/**
 * Provider contract for JARVIS.
 *
 * A real implementation must be backed by an explicitly authorized provider.
 * Never put API keys, OAuth client secrets, or refresh tokens in source code.
 */
interface AiProvider {
    val displayName: String
    suspend fun reply(
        conversation: List<ConversationMessage>,
        userMessage: String
    ): AiResult
}

data class ConversationMessage(
    val role: Role,
    val content: String
) {
    enum class Role { USER, ASSISTANT }
}

sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Unavailable(val reason: String) : AiResult()
}
