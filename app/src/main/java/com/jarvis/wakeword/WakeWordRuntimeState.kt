package com.jarvis.wakeword

/**
 * Process-local truth for the wake-word engine.
 *
 * SharedPreferences cannot prove that a foreground service survived process death.
 * This value intentionally resets to false when Android creates a new app process.
 */
internal object WakeWordRuntimeState {
    @Volatile
    private var listening: Boolean = false

    fun setListening(value: Boolean) {
        listening = value
    }

    fun isListening(): Boolean = listening
}
