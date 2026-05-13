package com.frank.voiceoverlay.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayBubbleServiceGateTest {
    @Test
    fun sync_startsServiceWhenOverlayBecomesEnabled() {
        val events = mutableListOf<String>()
        val gate = OverlayBubbleServiceGate(
            startService = { events += "start" },
            stopService = { events += "stop" },
        )

        gate.sync(overlayEnabled = true)

        assertEquals(listOf("start"), events)
    }

    @Test
    fun sync_doesNotRepeatActionWhenStateIsUnchanged() {
        val events = mutableListOf<String>()
        val gate = OverlayBubbleServiceGate(
            startService = { events += "start" },
            stopService = { events += "stop" },
        )

        gate.sync(overlayEnabled = true)
        gate.sync(overlayEnabled = true)
        gate.sync(overlayEnabled = false)
        gate.sync(overlayEnabled = false)

        assertEquals(listOf("start", "stop"), events)
    }
}
