package com.example.yolo_homes.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Brand loading animation: two pulsing rings + a sweeping arc, drawn on a Canvas.
 * Dependency-free (no Lottie asset needed) and reusable anywhere a spinner is wanted.
 */
@Composable
fun PulseLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 72.dp,
    content: (@Composable () -> Unit)? = null
) {
    val transition = rememberInfiniteTransition(label = "loader")

    val sweepRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 5f
            val dim = this.size.minDimension
            // Expanding pulse ring (fades as it grows)
            val r1 = dim / 2f * (0.45f + 0.55f * pulse)
            drawCircle(
                color = color.copy(alpha = (1f - pulse) * 0.5f),
                radius = r1,
                style = Stroke(width = stroke)
            )
            // Static faint base ring
            drawCircle(
                color = color.copy(alpha = 0.18f),
                radius = dim / 2f * 0.7f,
                style = Stroke(width = stroke)
            )
            // Sweeping arc
            val inset = dim * 0.15f
            rotate(sweepRotation) {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(dim - inset * 2, dim - inset * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content?.invoke()
    }
}
