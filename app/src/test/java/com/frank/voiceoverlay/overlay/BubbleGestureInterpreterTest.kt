package com.frank.voiceoverlay.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleGestureInterpreterTest {
    @Test
    fun secondTapWithinThreshold_emitsDoubleTap() {
        val interpreter = BubbleGestureInterpreter(doubleTapWindowMillis = 250)

        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1000))
        assertEquals(BubbleGesture.DoubleTap, interpreter.onTap(1180))
    }

    @Test
    fun secondTapAtThresholdBoundary_emitsDoubleTap() {
        val interpreter = BubbleGestureInterpreter(doubleTapWindowMillis = 250)

        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1000))
        assertEquals(BubbleGesture.DoubleTap, interpreter.onTap(1250))
    }

    @Test
    fun longGapTap_allowsPendingTapToResolveBeforeNextTap() {
        val interpreter = BubbleGestureInterpreter(doubleTapWindowMillis = 250)

        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1000))
        assertEquals(BubbleGesture.SingleTapConfirmed, interpreter.resolvePendingTap(1251))
        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1400))
    }

    @Test
    fun tapAfterDoubleTap_startsFreshPendingTap() {
        val interpreter = BubbleGestureInterpreter(doubleTapWindowMillis = 250)

        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1000))
        assertEquals(BubbleGesture.DoubleTap, interpreter.onTap(1180))
        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1600))
    }

    @Test
    fun resolvePendingTap_requiresTimeoutAndClearsPendingState() {
        val interpreter = BubbleGestureInterpreter(doubleTapWindowMillis = 250)

        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1000))
        assertEquals(null, interpreter.resolvePendingTap(1200))
        assertEquals(BubbleGesture.SingleTapConfirmed, interpreter.resolvePendingTap(1250))
        assertEquals(null, interpreter.resolvePendingTap(1400))
    }

    @Test
    fun reset_clearsPendingTapWithoutEmittingGesture() {
        val interpreter = BubbleGestureInterpreter(doubleTapWindowMillis = 250)

        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1000))

        interpreter.reset()

        assertEquals(null, interpreter.resolvePendingTap(1300))
        assertEquals(BubbleGesture.SingleTapPending, interpreter.onTap(1400))
    }
}
