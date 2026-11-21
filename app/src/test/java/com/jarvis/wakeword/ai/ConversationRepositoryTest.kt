package com.jarvis.wakeword.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        val repository = ConversationRepository(object : AiProvider {
            override val displayName = "Test provider"

            override suspend fun reply(
                conversation: List<ConversationMessage>,
                userMessage: String
            ): AiResult = AiResult.Success("Buenos días.")
        })

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
}
