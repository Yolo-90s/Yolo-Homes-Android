package com.example.yolo_homes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

data class ChartEntry(val label: String, val value: Double)

/** Minimal animated bar chart drawn on a Canvas — no third-party chart dependency. */
@Composable
fun BarChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 180.dp
) {
    if (entries.isEmpty()) return
    val max = (entries.maxOf { it.value }).coerceAtLeast(1.0)
    val progress by animateFloatAsState(targetValue = 1f, animationSpec = tween(800), label = "bar")

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 4.dp)
        ) {
            val count = entries.size
            val slot = size.width / count
            val barWidth = slot * 0.5f
            entries.forEachIndexed { i, e ->
                val ratio = (e.value / max).toFloat() * progress
                val barH = size.height * ratio
                val left = i * slot + (slot - barWidth) / 2
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barH),
                    size = androidx.compose.ui.geometry.Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 3)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            entries.forEach {
                Text(
                    it.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Smooth line/area trend chart. */
@Composable
fun LineChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 180.dp
) {
    if (entries.size < 2) {
        if (entries.size == 1) BarChart(entries, modifier, lineColor, height)
        return
    }
    val max = entries.maxOf { it.value }.coerceAtLeast(1.0)
    val min = entries.minOf { it.value }
    val range = (max - min).coerceAtLeast(1.0)
    val progress by animateFloatAsState(targetValue = 1f, animationSpec = tween(900), label = "line")

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            val stepX = size.width / (entries.size - 1)
            fun y(v: Double) = (size.height * (1f - ((v - min) / range).toFloat())).coerceIn(0f, size.height)

            val line = Path()
            val area = Path()
            entries.forEachIndexed { i, e ->
                val x = i * stepX
                val yy = y(e.value) + (size.height - y(e.value)) * (1f - progress)
                if (i == 0) {
                    line.moveTo(x, yy); area.moveTo(x, size.height); area.lineTo(x, yy)
                } else {
                    line.lineTo(x, yy); area.lineTo(x, yy)
                }
            }
            area.lineTo((entries.size - 1) * stepX, size.height)
            area.close()

            drawPath(area, color = lineColor.copy(alpha = 0.12f))
            drawPath(line, color = lineColor, style = Stroke(width = 6f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            entries.forEach {
                Text(
                    it.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
