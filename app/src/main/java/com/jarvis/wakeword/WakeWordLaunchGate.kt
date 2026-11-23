package com.jarvis.wakeword

/**
 * Prevents a delayed wake-word callback from launching another app after
 * its listening session was stopped, failed, or replaced by a newer session.
 */
internal object WakeWordLaunchGate {
    fun canLaunch(
        detectedGeneration: Long,
        currentGeneration: Long,
        terminalError: Boolean
    ): Boolean = detectedGeneration == currentGeneration && !terminalError
}
