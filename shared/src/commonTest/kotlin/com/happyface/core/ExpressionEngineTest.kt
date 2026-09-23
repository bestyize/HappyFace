package com.happyface.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpressionEngineTest {
    @Test
    fun newIntentMovesContinuouslyFromCurrentFrame() {
        val engine = ExpressionEngine(seed = 7)
        engine.setIntent(ExpressionIntent(Mood.HAPPY, intensity = 1.0))
        val before = engine.currentFrame()
        val after = engine.tick(1.0 / 60.0)
        assertTrue(after.smile > before.smile)
        assertTrue(after.smile < 1.0)
    }

    @Test
    fun matchingSeedsProduceSameMicroMotion() {
        val first = ExpressionEngine(seed = 23)
        val second = ExpressionEngine(seed = 23)
        repeat(400) {
            assertEquals(first.tick(1.0 / 60.0), second.tick(1.0 / 60.0))
        }
    }

    @Test
    fun remoteIntentIsClampedBeforeAnimation() {
        val engine = ExpressionEngine()
        engine.setIntent(ExpressionIntent(Mood.CURIOUS, 99.0, 99.0, -99.0))
        repeat(300) { engine.tick(1.0 / 60.0) }
        assertTrue(engine.currentFrame().gazeX <= 1.0)
        assertTrue(engine.currentFrame().gazeY >= -1.0)
    }
}

