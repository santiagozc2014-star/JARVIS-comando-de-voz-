package com.jarvis.wakeword

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchRequestPolicyTest {

    @Test
    fun chatGPTRequestAccepted_doesNotTryFallbacks() {
        repeat(5) {
            val gateway = FakeGateway(chatGPT = true, assistant = true, playStore = true)

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.CHATGPT_REQUESTED, result)
            assertEquals(listOf("chatGPT"), gateway.calls)
        }
    }

    @Test
    fun chatGPTUnavailable_assistantRequestAccepted() {
        repeat(5) {
            val gateway = FakeGateway(chatGPT = false, assistant = true, playStore = true)

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.ASSIST_REQUESTED, result)
            assertEquals(listOf("chatGPT", "assistant"), gateway.calls)
        }
    }

    @Test
    fun chatGPTAndAssistantUnavailable_playStoreFallbackAccepted() {
        repeat(5) {
            val gateway = FakeGateway(chatGPT = false, assistant = false, playStore = true)

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.PLAY_STORE_REQUESTED, result)
            assertEquals(listOf("chatGPT", "assistant", "playStore"), gateway.calls)
        }
    }

    @Test
    fun allLaunchRequestsFail_returnsFailure() {
        repeat(5) {
            val gateway = FakeGateway(chatGPT = false, assistant = false, playStore = false)

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.FAILED, result)
            assertEquals(listOf("chatGPT", "assistant", "playStore"), gateway.calls)
        }
    }

    @Test
    fun thrownChatGPTLaunchException_usesAssistantFallback() {
        repeat(5) {
            val gateway = object : LaunchRequestGateway {
                val calls = mutableListOf<String>()
                override fun requestChatGPT(): Boolean {
                    calls += "chatGPT"
                    throw IllegalStateException("simulated Android launch failure")
                }
                override fun requestAssistant(): Boolean {
                    calls += "assistant"
                    return true
                }
                override fun requestPlayStore(): Boolean {
                    calls += "playStore"
                    return true
                }
            }

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.ASSIST_REQUESTED, result)
            assertEquals(listOf("chatGPT", "assistant"), gateway.calls)
        }
    }

    private class FakeGateway(
        private val chatGPT: Boolean,
        private val assistant: Boolean,
        private val playStore: Boolean
    ) : LaunchRequestGateway {
        val calls = mutableListOf<String>()

        override fun requestAssistant(): Boolean {
            calls += "assistant"
            return assistant
        }

        override fun requestChatGPT(): Boolean {
            calls += "chatGPT"
            return chatGPT
        }

        override fun requestPlayStore(): Boolean {
            calls += "playStore"
            return playStore
        }
    }
}
