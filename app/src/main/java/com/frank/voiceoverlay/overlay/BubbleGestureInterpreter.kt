package com.frank.voiceoverlay.overlay

sealed interface BubbleGesture {
    data object SingleTapPending : BubbleGesture
    data object DoubleTap : BubbleGesture
}

class BubbleGestureInterpreter(
    private val doubleTapWindowMillis: Long,
) {
    private var pendingTapAtMillis: Long? = null

    fun onTap(timestampMillis: Long): BubbleGesture {
        val previousTapAtMillis = pendingTapAtMillis
        return if (
            previousTapAtMillis != null &&
            timestampMillis - previousTapAtMillis <= doubleTapWindowMillis
        ) {
            pendingTapAtMillis = null
            BubbleGesture.DoubleTap
        } else {
            pendingTapAtMillis = timestampMillis
            BubbleGesture.SingleTapPending
        }
    }
}
