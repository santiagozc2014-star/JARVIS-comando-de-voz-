package com.jarvis.wakeword

/**
 * Android added the microphone foreground-service type in API 30.
 * Apps targeting API 30+ must pass it both in the manifest and startForeground().
 */
internal object ForegroundServiceTypePolicy {
    fun requiresExplicitMicrophoneType(apiLevel: Int): Boolean = apiLevel >= 30
}
