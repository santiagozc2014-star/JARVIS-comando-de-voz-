package com.jarvis.wakeword

/**
 * Describes what Android accepted, not whether the target app became visible.
 * Background activity launches can be blocked without a synchronous error.
 */
internal enum class LaunchRequestResult(val userMessage: String) {
    ASSIST_REQUESTED("Android recibió la solicitud del asistente. La apertura no está confirmada."),
    CHATGPT_REQUESTED("Android recibió la solicitud de abrir ChatGPT. La apertura no está confirmada."),
    PLAY_STORE_REQUESTED("Se solicitó abrir Play Store porque ChatGPT no está disponible."),
    FAILED("No se pudo solicitar la apertura del asistente ni de ChatGPT.")
}

internal interface LaunchRequestGateway {
    fun requestAssistant(): Boolean
    fun requestChatGPT(): Boolean
    fun requestPlayStore(): Boolean
}

internal object LaunchRequestPolicy {
    fun execute(gateway: LaunchRequestGateway): LaunchRequestResult {
        if (safeAttempt { gateway.requestAssistant() }) {
            return LaunchRequestResult.ASSIST_REQUESTED
        }
        if (safeAttempt { gateway.requestChatGPT() }) {
            return LaunchRequestResult.CHATGPT_REQUESTED
        }
        if (safeAttempt { gateway.requestPlayStore() }) {
            return LaunchRequestResult.PLAY_STORE_REQUESTED
        }
        return LaunchRequestResult.FAILED
    }

    private inline fun safeAttempt(action: () -> Boolean): Boolean =
        try {
            action()
        } catch (_: Exception) {
            false
        }
}
