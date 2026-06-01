package com.example.yolo_homes.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** A horizontally-sweeping shimmer brush for skeleton placeholders. */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val colors = listOf(
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
    return Brush.linearGradient(
        colors = colors,
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 0f)
    )
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerBrush())
    ) {}
}

/** A stack of shimmering rows used while lists load. */
@Composable
fun ShimmerList(rows: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        repeat(rows) {
            ShimmerBox(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(72.dp)
            )
        }
    }
}
