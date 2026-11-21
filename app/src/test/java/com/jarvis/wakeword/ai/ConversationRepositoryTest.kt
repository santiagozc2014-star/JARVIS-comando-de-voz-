package com.jarvis.wakeword.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationRepositoryTest {

    @Test
    fun unavailableProviderDoesNotPretendToReplyOrStoreFailedTurn() = runBlocking {
        val repository = ConversationRepository(NotConfiguredAiProvider())

        val result = repository.send("Hola, JARVIS")

        assertTrue(result is AiResult.Unavailable)
        assertTrue(repository.history().isEmpty())
    }

    @Test
    fun successfulProviderStoresUserAndAssistantMessages() = runBlocking {
        val repository = ConversationRepository(replyingProvider())

        val result = repository.send("Hola")

        assertEquals(AiResult.Success("Buenos días."), result)
        assertEquals(
            listOf(
                ConversationMessage(ConversationMessage.Role.USER, "Hola"),
                ConversationMessage(ConversationMessage.Role.ASSISTANT, "Buenos días.")
            ),
            repository.history()
        )
    }

    @Test
    fun blankMessageIsRejected() = runBlocking {
        val repository = ConversationRepository(NotConfiguredAiProvider())

        val result = repository.send("   ")

        assertTrue(result is AiResult.Unavailable)
        assertTrue(repository.history().isEmpty())
    }

    @Test
    fun historyIsOnlyPersistedAfterExplicitEnablement() = runBlocking {
        val store = FakeHistoryStore()
        val repository = ConversationRepository(replyingProvider(), store)

        repository.send("Primero")
        assertTrue(store.saved.isEmpty())

        assertTrue(repository.enablePersistence())
        repository.send("Segundo")

        assertEquals(4, store.saved.size)
        assertEquals("Segundo", store.saved[2].content)
    }

    @Test
    fun disablingPersistenceDeletesSavedCopyButKeepsCurrentSession() = runBlocking {
        val store = FakeHistoryStore()
        val repository = ConversationRepository(replyingProvider(), store)
        repository.enablePersistence()
        repository.send("Conservar en memoria")

        assertTrue(repository.disablePersistence())

        assertFalse(repository.isPersistenceEnabled)
        assertTrue(store.saved.isEmpty())
        assertEquals(2, repository.history().size)
    }

    @Test
    fun enablingPersistenceRestoresPreviouslySavedHistory() = runBlocking {
        val store = FakeHistoryStore()
        store.saved = listOf(
            ConversationMessage(ConversationMessage.Role.USER, "Anterior"),
            ConversationMessage(ConversationMessage.Role.ASSISTANT, "Respuesta anterior")
        )
        val repository = ConversationRepository(replyingProvider(), store)

        repository.enablePersistence()

        assertEquals(store.saved, repository.history())
    }

    private fun replyingProvider() = object : AiProvider {
        override val displayName = "Test provider"

        override suspend fun reply(
            conversation: List<ConversationMessage>,
            userMessage: String
        ): AiResult = AiResult.Success("Buenos días.")
    }

    private class FakeHistoryStore : ConversationHistoryStore {
        var saved: List<ConversationMessage> = emptyList()

        override suspend fun read(): List<ConversationMessage> = saved

        override suspend fun write(messages: List<ConversationMessage>) {
            saved = messages.toList()
        }

        override suspend fun clear() {
            saved = emptyList()
        }
    }
}
