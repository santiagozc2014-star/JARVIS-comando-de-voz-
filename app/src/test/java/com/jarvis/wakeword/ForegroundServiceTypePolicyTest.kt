package com.jarvis.wakeword

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundServiceTypePolicyTest {

    @Test
    fun api29_doesNotRequireMicrophoneTypeInStartForeground() {
        assertFalse(ForegroundServiceTypePolicy.requiresExplicitMicrophoneType(29))
    }

    @Test
    fun api30_requiresMicrophoneTypeInStartForeground() {
        assertTrue(ForegroundServiceTypePolicy.requiresExplicitMicrophoneType(30))
    }

    @Test
    fun newerApisStillRequireMicrophoneType() {
        assertTrue(ForegroundServiceTypePolicy.requiresExplicitMicrophoneType(34))
    }
}
