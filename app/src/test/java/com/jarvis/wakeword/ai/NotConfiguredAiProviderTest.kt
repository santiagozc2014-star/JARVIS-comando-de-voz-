package com.jarvis.wakeword.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotConfiguredAiProviderTest {

    @Test
    fun replyClearlyReportsThatNoProviderIsConfigured() = runBlocking {
        val provider = NotConfiguredAiProvider()

        val result = provider.reply(
            conversation = emptyList(),
            userMessage = "Hola, JARVIS"
        )

        assertEquals("ChatGPT (pendiente de conexión)", provider.displayName)
        assertTrue(result is AiResult.Unavailable)
        assertEquals(
            "El proveedor todavía no está autorizado. Tu mensaje no se ha enviado a ChatGPT.",
            (result as AiResult.Unavailable).reason
        )
    }

    @Test
    fun replyNeverReturnsASimulatedSuccess() = runBlocking {
        val provider = NotConfiguredAiProvider()

        val result = provider.reply(
            conversation = listOf(
                ConversationMessage(
                    role = ConversationMessage.Role.USER,
                    content = "Mensaje anterior"
                )
            ),
            userMessage = "Continuar"
        )

        assertTrue("El proveedor no configurado no debe simular una respuesta", result !is AiResult.Success)
    }
}
