package com.jarvis.wakeword.ai

/**
 * Explicit state prevents the UI from implying that ChatGPT is connected
 * before OAuth and a real Responses API request have been verified.
 */
enum class ProviderConnectionState {
    NOT_CONFIGURED,
    AUTHORIZATION_REQUIRED,
    CONNECTED,
    ERROR
}
