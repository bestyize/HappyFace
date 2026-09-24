package com.happyface.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.happyface.core.ExpressionEngine
import com.happyface.core.FaceFrame
import kotlin.math.min

/** First P0 preview: a complete screen face; ears always belong to external hardware. */
@Composable
fun FaceApp() {
    val engine = remember { ExpressionEngine() }
    var frame by remember { mutableStateOf(engine.currentFrame()) }
    LaunchedEffect(engine) {
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) frame = engine.tick((now - previous) / 1_000_000_000.0)
                previous = now
            }
        }
    }
    Canvas(
        Modifier.fillMaxSize().pointerInput(engine) {
            detectTapGestures { point ->
                engine.tap(
                    (point.x / size.width).toDouble(),
                    (point.y / size.height).toDouble(),
                )
            }
        },
    ) {
        drawRect(Color(0xFF080B12))
        // Match the full-screen face proportions used by the web preview.
        // The width constraint leaves room for the widest eye and its stroke.
        val unit = min(size.width / 4.25f, size.height / 2.55f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val drift = frame.breath.toFloat() * unit
        val spacing = unit * 1.18f
        val eyeY = center.y - unit * 0.22f + drift + frame.gazeY.toFloat() * unit * 0.08f
        val stroke = (unit * 0.105f).coerceAtLeast(2f)
        drawEye(center.x - spacing, eyeY, unit, stroke, frame)
        drawEye(center.x + spacing, eyeY, unit, stroke, frame)

        val mouth = Path().apply {
            val x = center.x + frame.gazeX.toFloat() * unit * 0.035f
            val y = center.y + unit * 0.55f + drift
            moveTo(x - unit * 0.18f, y)
            quadraticTo(x - unit * 0.10f, y + unit * (0.08f + frame.smile.toFloat() * 0.11f), x, y)
            quadraticTo(x + unit * 0.10f, y + unit * (0.08f + frame.smile.toFloat() * 0.11f), x + unit * 0.18f, y)
        }
        drawPath(mouth, Color(0xFFF6FAFF), style = Stroke(width = stroke * 0.62f, cap = StrokeCap.Round))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEye(
    centerX: Float, centerY: Float, unit: Float, stroke: Float, frame: FaceFrame,
) {
    val center = Offset(centerX + frame.gazeX.toFloat() * unit * 0.10f, centerY)
    val radius = unit * 0.50f * frame.eyeRoundness.toFloat()
    val openness = frame.eyeOpen.toFloat().coerceIn(0.04f, 1f)
    if (openness < 0.18f) {
        drawLine(Color(0xFFBCEBFF), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), stroke, cap = StrokeCap.Round)
    } else {
        drawOval(
            color = Color(0xFFBCEBFF),
            topLeft = Offset(center.x - radius, center.y - radius * openness),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * openness * 2),
            style = Stroke(width = stroke),
        )
    }
}
