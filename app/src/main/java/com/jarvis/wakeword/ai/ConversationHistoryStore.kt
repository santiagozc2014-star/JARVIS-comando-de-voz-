package com.jarvis.wakeword.ai

/**
 * Storage boundary for conversation history.
 * Implementations must protect data at rest and remain local to the device.
 */
interface ConversationHistoryStore {
    suspend fun read(): List<ConversationMessage>
    suspend fun write(messages: List<ConversationMessage>)
    suspend fun clear()
}
