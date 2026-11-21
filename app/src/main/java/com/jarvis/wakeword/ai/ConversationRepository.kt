package com.jarvis.wakeword.ai

/**
 * Owns the in-memory conversation history and delegates responses to a provider.
 * Persistent history is opt-in and delegated to a protected local store.
 */
class ConversationRepository(
    private val provider: AiProvider,
    private val historyStore: ConversationHistoryStore? = null
) {
    private val messages = mutableListOf<ConversationMessage>()
    var isPersistenceEnabled: Boolean = false
        private set
    var persistenceWarning: String? = null
        private set

    val providerName: String
        get() = provider.displayName

    fun history(): List<ConversationMessage> = messages.toList()

    /**
     * Enables local persistence and restores a previous transcript, if present.
     * If the current session already has messages, they take precedence over an
     * empty store (for example, after the user disabled and re-enabled saving).
     */
    suspend fun enablePersistence(): Boolean {
        val store = historyStore ?: return false
        val restored = store.read()
        if (messages.isEmpty() || restored.isNotEmpty()) {
            messages.clear()
            messages.addAll(restored)
        }
        isPersistenceEnabled = true
        persistenceWarning = null
        if (messages.isNotEmpty()) persistCurrentHistory()
        return true
    }

    /**
     * Stops future writes and deletes the saved transcript. The current in-memory
     * session remains available until the screen is closed or history is cleared.
     */
    suspend fun disablePersistence(): Boolean {
        val store = historyStore ?: return false
        store.clear()
        isPersistenceEnabled = false
        persistenceWarning = null
        return true
    }

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
            if (isPersistenceEnabled) persistCurrentHistory()
        }
        return result
    }

    suspend fun clearHistory() {
        historyStore?.clear()
        messages.clear()
        persistenceWarning = null
    }

    private suspend fun persistCurrentHistory() {
        try {
            historyStore?.write(messages.toList())
            persistenceWarning = null
        } catch (_: Exception) {
            persistenceWarning = "No se pudo guardar el historial cifrado en este dispositivo."
        }
    }
}
