package com.jarvis.wakeword

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WakeWordRuntimeStateTest {

    @Before
    fun resetRuntimeState() {
        WakeWordRuntimeState.setListening(false)
    }

    @Test
    fun processStartsWithNoActiveListeningService() {
        assertFalse(WakeWordRuntimeState.isListening())
    }

    @Test
    fun successfulServiceStartMarksListeningInCurrentProcess() {
        WakeWordRuntimeState.setListening(true)

        assertTrue(WakeWordRuntimeState.isListening())
    }

    @Test
    fun serviceStopOrFailureClearsListeningState() {
        WakeWordRuntimeState.setListening(true)

        WakeWordRuntimeState.setListening(false)

        assertFalse(WakeWordRuntimeState.isListening())
    }
}
