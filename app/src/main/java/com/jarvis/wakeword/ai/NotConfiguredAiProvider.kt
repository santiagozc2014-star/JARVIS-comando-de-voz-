package com.jarvis.wakeword.ai

/**
 * Safe default until a real provider has been authorized and configured.
 * It never pretends to contact ChatGPT or another remote model.
 */
class NotConfiguredAiProvider : AiProvider {
    override val displayName: String = "ChatGPT (pendiente de conexión)"

    override suspend fun reply(
        conversation: List<ConversationMessage>,
        userMessage: String
    ): AiResult = AiResult.Unavailable(
        "El proveedor todavía no está autorizado. Tu mensaje no se ha enviado a ChatGPT."
    )
}
