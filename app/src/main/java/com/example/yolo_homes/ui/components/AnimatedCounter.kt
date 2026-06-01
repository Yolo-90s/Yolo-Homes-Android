package com.example.yolo_homes.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Counts up from 0 to [target] when first composed — the SaaS "number ticks up" effect.
 */
@Composable
fun AnimatedCounterText(
    target: Double,
    modifier: Modifier = Modifier,
    style: TextStyle,
    formatter: (Double) -> String
) {
    val animated by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
        label = "counter"
    )
    androidx.compose.material3.Text(
        text = formatter(animated.toDouble()),
        modifier = modifier,
        style = style
    )
}
