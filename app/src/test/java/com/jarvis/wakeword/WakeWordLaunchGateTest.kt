package com.jarvis.wakeword

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordLaunchGateTest {

    @Test
    fun currentHealthySessionMayLaunch() {
        assertTrue(WakeWordLaunchGate.canLaunch(7L, 7L, false))
    }

    @Test
    fun stoppedOrReplacedSessionCannotLaunch() {
        assertFalse(WakeWordLaunchGate.canLaunch(7L, 8L, false))
    }

    @Test
    fun terminalErrorBlocksLaunchEvenWhenGenerationMatches() {
        assertFalse(WakeWordLaunchGate.canLaunch(7L, 7L, true))
    }
}
