package com.frank.voiceoverlay.overlay

class OverlayBubbleServiceGate(
    private val startService: () -> Unit,
    private val stopService: () -> Unit,
) {
    private var lastAppliedState: Boolean? = null

    fun sync(overlayEnabled: Boolean) {
        if (lastAppliedState == overlayEnabled) {
            return
        }
        lastAppliedState = overlayEnabled
        if (overlayEnabled) {
            startService()
        } else {
            stopService()
        }
    }
}
