package com.jarvis.wakeword.ai

/**
 * Owns the in-memory conversation history and delegates responses to a provider.
 * Persistence and remote authorization will be separate, explicit stages.
 */
class ConversationRepository(
    private val provider: AiProvider
) {
    private val messages = mutableListOf<ConversationMessage>()

    val providerName: String
        get() = provider.displayName

    fun history(): List<ConversationMessage> = messages.toList()

    suspend fun send(userMessage: String): AiResult {
        val normalized = userMessage.trim()
        if (normalized.isEmpty()) {
            return AiResult.Unavailable("El mensaje está vacío.")
        }

        val previousMessages = messages.toList()
        val result = provider.reply(previousMessages, normalized)
        if (result is AiResult.Success) {
            messages += ConversationMessage(
                role = ConversationMessage.Role.USER,
                content = normalized
            )
            messages += ConversationMessage(
                role = ConversationMessage.Role.ASSISTANT,
                content = result.text
            )
        }
        return result
    }

    fun clear() {
        messages.clear()
    }
}
