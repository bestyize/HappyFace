package com.happyface.core

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** All coordinates and expression controls use normalized ranges, independent of pixels. */
data class ExpressionIntent(
    val mood: Mood = Mood.CALM,
    val intensity: Double = 0.5,
    val gazeX: Double = 0.0,
    val gazeY: Double = 0.0,
)

enum class Mood { CALM, CURIOUS, HAPPY, SHY, THINKING, SLEEPY }

data class FaceFrame(
    val eyeOpen: Double = 1.0,
    val eyeRoundness: Double = 1.0,
    val smile: Double = 0.0,
    val gazeX: Double = 0.0,
    val gazeY: Double = 0.0,
    val breath: Double = 0.0,
)

/**
 * Frame-rate independent local animation. Cloud responses change intent, never individual frames.
 * Pass elapsed seconds to [tick] from the display's frame clock.
 */
class ExpressionEngine(seed: Int = 1) {
    private val random = Random(seed)
    private var intent = ExpressionIntent()
    private var frame = FaceFrame()
    private var elapsed = 0.0
    private var nextBlink = blinkDelay()
    private var blinkAge = -1.0
    private var relaxedEyeOpen = 1.0

    fun setIntent(next: ExpressionIntent) {
        intent = next.copy(
            intensity = next.intensity.coerceIn(0.0, 1.0),
            gazeX = next.gazeX.coerceIn(-1.0, 1.0),
            gazeY = next.gazeY.coerceIn(-1.0, 1.0),
        )
    }

    fun tap(x: Double, y: Double) {
        setIntent(ExpressionIntent(Mood.CURIOUS, 0.75, x * 2 - 1, y * 2 - 1))
    }

    fun currentFrame(): FaceFrame = frame

    fun tick(deltaSeconds: Double): FaceFrame {
        val dt = deltaSeconds.coerceIn(0.0, 0.1)
        elapsed += dt
        if (blinkAge < 0.0 && elapsed >= nextBlink) {
            blinkAge = 0.0
            nextBlink = elapsed + blinkDelay()
        }
        if (blinkAge >= 0.0) blinkAge += dt
        val blink = when {
            blinkAge < 0.0 -> 1.0
            blinkAge < 0.09 -> 1.0 - blinkAge / 0.09
            blinkAge < 0.19 -> (blinkAge - 0.09) / 0.10
            else -> { blinkAge = -1.0; 1.0 }
        }.coerceIn(0.04, 1.0)

        val amount = intent.intensity
        val roundness = when (intent.mood) {
            Mood.CALM -> 1.0
            Mood.CURIOUS -> 1.12
            Mood.HAPPY -> 0.65
            Mood.SHY -> 0.82
            Mood.THINKING -> 0.88
            Mood.SLEEPY -> 0.45
        }
        val smile = when (intent.mood) {
            Mood.HAPPY -> 0.75 + amount * 0.25
            Mood.SHY -> 0.35
            Mood.CURIOUS -> 0.12
            Mood.SLEEPY -> 0.05
            else -> 0.0
        }
        val open = if (intent.mood == Mood.SLEEPY) 0.55 else 1.0
        val blend = 1.0 - exp(-dt * 7.5)
        fun move(from: Double, to: Double) = from + (to - from) * blend
        relaxedEyeOpen = move(relaxedEyeOpen, open)
        frame = FaceFrame(
            eyeOpen = relaxedEyeOpen * blink,
            eyeRoundness = move(frame.eyeRoundness, roundness),
            smile = move(frame.smile, smile),
            gazeX = move(frame.gazeX, intent.gazeX),
            gazeY = move(frame.gazeY, intent.gazeY),
            breath = sin(elapsed * 2 * PI / 3.7) * 0.012,
        )
        return frame
    }

    private fun blinkDelay() = 2.2 + random.nextDouble() * 3.4
}
