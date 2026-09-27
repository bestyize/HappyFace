package com.happyface.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpressionEngineTest {
    @Test
    fun interruptionStartsAtTheCurrentFrameAndShapeGroupsStayNormalized() {
        val engine = ExpressionEngine(seed = 7)
        engine.setIntent(ExpressionIntent(Mood.HAPPY, intensity = 1.0, holdMs = 5_000))
        engine.tick(0.4)
        val before = engine.currentFrame()
        engine.setIntent(ExpressionIntent(Mood.CURIOUS, intensity = 1.0, holdMs = 5_000))
        assertEquals(before, engine.currentFrame())

        val after = engine.tick(1.0 / 60.0)
        assertTrue(after.eyeArch < before.eyeArch)
        assertTrue(after.eyeCrescent > before.eyeCrescent)
        assertTrue(after.eyeCrescent < 1.0)
        assertEquals(1.0, after.eyeRing + after.eyeArch + after.eyeCrescent + after.eyeHalf + after.eyeSleep, 1e-9)
        assertEquals(1.0, after.mouthW + after.mouthO + after.mouthFlat, 1e-9)
    }

    @Test
    fun touchTakesPriorityThenReturnsToStillValidDialogueThenIdle() {
        val engine = ExpressionEngine(seed = 3)
        engine.setIntent(ExpressionIntent(Mood.HAPPY, intensity = 1.0, holdMs = 8_000))
        engine.tick(1.0)
        assertTrue(engine.currentFrame().eyeArch > 0.99)

        engine.tap(0.1, 0.5)
        engine.tick(0.4)
        assertTrue(engine.currentFrame().eyeCrescent > 0.8)
        assertTrue(engine.currentFrame().gazeX < 0.0)

        engine.tick(2.0)
        assertTrue(engine.currentFrame().eyeArch > 0.99)
        engine.tick(6.0)
        assertTrue(engine.currentFrame().eyeRing > 0.99)
    }

    @Test
    fun priorityCanBeClearedWithoutErasingDialogue() {
        val engine = ExpressionEngine()
        engine.setIntent(ExpressionIntent(Mood.HAPPY, intensity = 1.0, holdMs = 5_000))
        engine.setIntent(ExpressionIntent(Mood.SLEEPY, intensity = 1.0), IntentSource.SYSTEM)
        engine.tick(0.5)
        assertTrue(engine.currentFrame().eyeSleep > 0.9)
        engine.clear(IntentSource.SYSTEM)
        engine.tick(0.5)
        assertTrue(engine.currentFrame().eyeArch > 0.9)
    }

    @Test
    fun differentFrameRatesProduceTheSameTimedTransition() {
        val fast = ExpressionEngine(seed = 11)
        val slow = ExpressionEngine(seed = 11)
        val intent = ExpressionIntent(Mood.HAPPY, intensity = 0.8)
        fast.setIntent(intent)
        slow.setIntent(intent)
        repeat(60) { fast.tick(1.0 / 60.0) }
        repeat(30) { slow.tick(1.0 / 30.0) }
        assertEquals(fast.currentFrame().eyeArch, slow.currentFrame().eyeArch, 1e-9)
        assertEquals(fast.currentFrame().breath, slow.currentFrame().breath, 1e-9)
    }

    @Test
    fun matchingSeedsProduceTheSameMicroMotion() {
        val first = ExpressionEngine(seed = 23)
        val second = ExpressionEngine(seed = 23)
        repeat(500) {
            assertEquals(first.tick(1.0 / 60.0), second.tick(1.0 / 60.0))
        }
    }

    @Test
    fun invalidNumbersAreRejectedAndFiniteInputsClamped() {
        val engine = ExpressionEngine()
        assertFalse(engine.setIntent(ExpressionIntent(Mood.CURIOUS, Double.NaN)))
        assertTrue(engine.setIntent(ExpressionIntent(Mood.CURIOUS, 99.0, 99.0, -99.0)))
        engine.tick(0.5)
        assertTrue(engine.currentFrame().gazeX <= 1.0)
        assertTrue(engine.currentFrame().gazeY >= -1.0)
        assertTrue(engine.currentFrame().eyeCrescent <= 1.0)
    }

    @Test
    fun resumeExpiresOldIntentsAndReducedMotionKeepsInteraction() {
        val engine = ExpressionEngine()
        engine.setIntent(ExpressionIntent(Mood.HAPPY, intensity = 1.0, holdMs = 500))
        engine.tick(60.0)
        assertTrue(engine.currentFrame().eyeRing > 0.99)
        assertEquals(1.0, engine.currentFrame().eyeOpen)
        engine.setMotionScale(0.0)
        engine.tap(1.0, 0.5)
        engine.tick(0.2)
        assertTrue(engine.currentFrame().eyeCrescent > 0.0)
        assertEquals(0.0, engine.currentFrame().breath)
        assertEquals(1.0, engine.currentFrame().eyeOpen)
    }
}
