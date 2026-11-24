package com.jarvis.wakeword

import android.content.pm.ServiceInfo
import android.os.Build

/**
 * Returns the foreground-service type supported by the current platform.
 *
 * The microphone service type was introduced in Android 11 (API 30).
 * Older supported releases must continue to receive the legacy type value 0.
 */
internal object WakeWordForegroundServiceType {
    fun forSdk(sdkInt: Int): Int =
        if (sdkInt >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
}
