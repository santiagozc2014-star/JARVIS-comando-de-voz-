package com.jarvis.wakeword

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchRequestPolicyTest {

    @Test
    fun assistantRequestAccepted_doesNotTryFallbacks() {
        repeat(5) {
            val gateway = FakeGateway(assistant = true, chatGPT = true, playStore = true)

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.ASSIST_REQUESTED, result)
            assertEquals(listOf("assistant"), gateway.calls)
        }
    }

    @Test
    fun assistantUnavailable_chatGPTRequestAccepted() {
        repeat(5) {
            val gateway = FakeGateway(assistant = false, chatGPT = true, playStore = true)

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.CHATGPT_REQUESTED, result)
            assertEquals(listOf("assistant", "chatGPT"), gateway.calls)
        }
    }

    @Test
    fun assistantAndChatGPTUnavailable_playStoreFallbackAccepted() {
        repeat(5) {
            val gateway = FakeGateway(assistant = false, chatGPT = false, playStore = true)

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.PLAY_STORE_REQUESTED, result)
            assertEquals(listOf("assistant", "chatGPT", "playStore"), gateway.calls)
        }
    }

    @Test
    fun allLaunchRequestsFail_returnsFailure() {
        repeat(5) {
            val gateway = FakeGateway(assistant = false, chatGPT = false, playStore = false)

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.FAILED, result)
            assertEquals(listOf("assistant", "chatGPT", "playStore"), gateway.calls)
        }
    }

    @Test
    fun thrownLaunchException_usesNextRoute() {
        repeat(5) {
            val gateway = object : LaunchRequestGateway {
                val calls = mutableListOf<String>()
                override fun requestAssistant(): Boolean {
                    calls += "assistant"
                    throw IllegalStateException("simulated Android launch failure")
                }
                override fun requestChatGPT(): Boolean {
                    calls += "chatGPT"
                    return true
                }
                override fun requestPlayStore(): Boolean {
                    calls += "playStore"
                    return true
                }
            }

            val result = LaunchRequestPolicy.execute(gateway)

            assertEquals(LaunchRequestResult.CHATGPT_REQUESTED, result)
            assertEquals(listOf("assistant", "chatGPT"), gateway.calls)
        }
    }

    private class FakeGateway(
        private val assistant: Boolean,
        private val chatGPT: Boolean,
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
