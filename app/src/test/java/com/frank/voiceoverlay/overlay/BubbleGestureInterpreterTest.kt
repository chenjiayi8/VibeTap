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
}
