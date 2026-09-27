package com.happyface.core

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Semantic controls are independent of pixels, platform, and cloud provider. */
data class ExpressionIntent(
    val mood: Mood = Mood.CALM,
    val intensity: Double = 0.5,
    val gazeX: Double = 0.0,
    val gazeY: Double = 0.0,
    val holdMs: Long = 2_400,
)

enum class Mood { CALM, CURIOUS, HAPPY, SHY, THINKING, SLEEPY }

/** Source is assigned by trusted local code; a model response never chooses its own priority. */
enum class IntentSource(val priority: Int) {
    DIALOGUE(1), TOUCH(2), SYSTEM(3),
}

/**
 * One immutable render snapshot. Shape weights remain in [0, 1] and sum to one
 * within each eye/mouth group; the renderer can blend paths instead of swapping images.
 * eyeOpen includes an independent blink, while breath and gaze keep moving in idle.
 */
data class FaceFrame(
    val eyeOpen: Double = 1.0,
    val eyeRoundness: Double = 1.0,
    val smile: Double = 0.0,
    val gazeX: Double = 0.0,
    val gazeY: Double = 0.0,
    val breath: Double = 0.0,
    val eyeRing: Double = 1.0,
    val eyeArch: Double = 0.0,
    val eyeCrescent: Double = 0.0,
    val eyeHalf: Double = 0.0,
    val eyeSleep: Double = 0.0,
    val mouthW: Double = 1.0,
    val mouthO: Double = 0.0,
    val mouthFlat: Double = 0.0,
    val blush: Double = 0.0,
)

private data class ActiveIntent(val intent: ExpressionIntent, val expiresAt: Double)

/**
 * Deterministic local animation. setIntent accepts high-level goals; tick owns every
 * visible frame. A higher-priority command temporarily covers lower-priority goals.
 * On expiry, the still-valid lower-priority goal returns; otherwise idle takes over.
 */
class ExpressionEngine(seed: Int = 1) {
    private val random = Random(seed)
    private val active = mutableMapOf<IntentSource, ActiveIntent>()
    private var frame = FaceFrame()
    private var elapsed = 0.0
    private var nextBlink = blinkDelay()
    private var blinkAge = -1.0
    private var relaxedEyeOpen = 1.0
    private var nextIdleLook = idleLookDelay()
    private var idleTargetX = 0.0
    private var idleTargetY = 0.0
    private var idleLookX = 0.0
    private var idleLookY = 0.0
    private var motionScale = 1.0

    /** Invalid numbers are rejected. Finite but out-of-range controls are clamped. */
    fun setIntent(next: ExpressionIntent, source: IntentSource = IntentSource.DIALOGUE): Boolean {
        if (!next.intensity.isFinite() || !next.gazeX.isFinite() || !next.gazeY.isFinite()) return false
        val safe = next.copy(
            intensity = next.intensity.coerceIn(0.0, 1.0),
            gazeX = next.gazeX.coerceIn(-1.0, 1.0),
            gazeY = next.gazeY.coerceIn(-1.0, 1.0),
            holdMs = next.holdMs.coerceIn(100L, 15_000L),
        )
        active[source] = ActiveIntent(safe, elapsed + safe.holdMs / 1_000.0)
        return true
    }

    fun clear(source: IntentSource) {
        active.remove(source)
    }

    fun tap(x: Double, y: Double) {
        if (!x.isFinite() || !y.isFinite()) return
        setIntent(
            ExpressionIntent(Mood.CURIOUS, 0.9, x * 2 - 1, y * 2 - 1, holdMs = 1_800),
            IntentSource.TOUCH,
        )
    }

    /** 0 disables autonomous micro motion; interaction and transitions still work. */
    fun setMotionScale(scale: Double) {
        if (scale.isFinite()) motionScale = scale.coerceIn(0.0, 1.0)
    }

    fun currentFrame(): FaceFrame = frame

    fun tick(deltaSeconds: Double): FaceFrame {
        // A resumed app advances deadlines once; no backlog of missed blinks is replayed.
        val dt = if (deltaSeconds.isFinite()) deltaSeconds.coerceIn(0.0, 60.0) else 0.0
        elapsed += dt
        val expired = active.filterValues { it.expiresAt <= elapsed }.keys.toList()
        expired.forEach { active.remove(it) }
        val selected = active.entries.maxByOrNull { it.key.priority }?.value?.intent
        val intent = selected ?: ExpressionIntent()
        val amount = if (selected == null || intent.mood == Mood.CALM) 0.0 else intent.intensity

        if (elapsed >= nextIdleLook) {
            idleTargetX = (random.nextDouble() * 2 - 1) * 0.10
            idleTargetY = (random.nextDouble() * 2 - 1) * 0.05
            nextIdleLook = elapsed + idleLookDelay()
        }
        val idleBlend = 1.0 - exp(-dt * 0.7)
        idleLookX += (idleTargetX - idleLookX) * idleBlend
        idleLookY += (idleTargetY - idleLookY) * idleBlend
        val idleFactor = if (selected == null) motionScale else 0.0

        if (elapsed >= nextBlink && blinkAge < 0.0) {
            blinkAge = 0.0
            nextBlink = elapsed + blinkDelay()
        }
        if (blinkAge >= 0.0) blinkAge += dt
        val blink = when {
            blinkAge < 0.0 -> 1.0
            blinkAge < 0.09 -> 1.0 - smoothstep(blinkAge / 0.09)
            blinkAge < 0.20 -> smoothstep((blinkAge - 0.09) / 0.11)
            else -> { blinkAge = -1.0; 1.0 }
        }.coerceIn(0.04, 1.0)

        val targetRing = 1.0 - amount
        val targetArch = if (intent.mood == Mood.HAPPY || intent.mood == Mood.SHY) amount else 0.0
        val targetCrescent = if (intent.mood == Mood.CURIOUS) amount else 0.0
        val targetHalf = if (intent.mood == Mood.THINKING) amount else 0.0
        val targetSleep = if (intent.mood == Mood.SLEEPY) amount else 0.0
        val targetMouthO = if (intent.mood == Mood.CURIOUS) amount else 0.0
        val targetMouthFlat = if (intent.mood == Mood.THINKING) amount else 0.0
        val roundness = when (intent.mood) {
            Mood.CALM -> 1.0
            Mood.CURIOUS -> 1.12
            Mood.HAPPY -> 0.65
            Mood.SHY -> 0.82
            Mood.THINKING -> 0.88
            Mood.SLEEPY -> 0.45
        }
        val smile = when (intent.mood) {
            Mood.HAPPY -> amount
            Mood.SHY -> amount * 0.35
            Mood.CURIOUS -> amount * 0.12
            Mood.SLEEPY -> amount * 0.05
            else -> 0.0
        }
        val eyeOpen = if (intent.mood == Mood.SLEEPY) 1.0 - amount * 0.45 else 1.0
        val blush = when (intent.mood) {
            Mood.HAPPY -> amount * 0.75
            Mood.SHY -> amount
            else -> 0.0
        }
        val blend = 1.0 - exp(-dt * 7.5)
        fun move(from: Double, to: Double) = from + (to - from) * blend
        relaxedEyeOpen = move(relaxedEyeOpen, eyeOpen)
        frame = FaceFrame(
            eyeOpen = relaxedEyeOpen * (1.0 + (blink - 1.0) * motionScale),
            eyeRoundness = move(frame.eyeRoundness, 1.0 + (roundness - 1.0) * amount),
            smile = move(frame.smile, smile),
            gazeX = move(frame.gazeX, (intent.gazeX + idleLookX * idleFactor).coerceIn(-1.0, 1.0)),
            gazeY = move(frame.gazeY, (intent.gazeY + idleLookY * idleFactor).coerceIn(-1.0, 1.0)),
            breath = sin(elapsed * 2 * PI / 3.7) * 0.012 * motionScale,
            eyeRing = move(frame.eyeRing, targetRing),
            eyeArch = move(frame.eyeArch, targetArch),
            eyeCrescent = move(frame.eyeCrescent, targetCrescent),
            eyeHalf = move(frame.eyeHalf, targetHalf),
            eyeSleep = move(frame.eyeSleep, targetSleep),
            mouthW = move(frame.mouthW, 1.0 - targetMouthO - targetMouthFlat),
            mouthO = move(frame.mouthO, targetMouthO),
            mouthFlat = move(frame.mouthFlat, targetMouthFlat),
            blush = move(frame.blush, blush),
        )
        return frame
    }

    private fun blinkDelay() = 2.2 + random.nextDouble() * 3.4
    private fun idleLookDelay() = 3.0 + random.nextDouble() * 4.0
    private fun smoothstep(t: Double): Double {
        val x = t.coerceIn(0.0, 1.0)
        return x * x * (3 - 2 * x)
    }
}
