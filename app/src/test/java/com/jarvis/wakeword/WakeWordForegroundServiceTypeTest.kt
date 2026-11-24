package com.jarvis.wakeword

import android.content.pm.ServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeWordForegroundServiceTypeTest {

    @Test
    fun api29AndEarlierUseLegacyForegroundServiceType() {
        assertEquals(0, WakeWordForegroundServiceType.forSdk(29))
    }

    @Test
    fun api30UsesMicrophoneForegroundServiceType() {
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            WakeWordForegroundServiceType.forSdk(30)
        )
    }

    @Test
    fun newerApisKeepMicrophoneForegroundServiceType() {
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            WakeWordForegroundServiceType.forSdk(34)
        )
    }
}
