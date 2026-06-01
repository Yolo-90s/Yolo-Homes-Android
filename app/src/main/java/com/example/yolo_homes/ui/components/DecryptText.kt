package com.example.yolo_homes.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * AI "decryption" boot effect: each glyph scrambles through encrypted symbols, then locks
 * into the target character left-to-right with a subtle white glow. Calls [onComplete] once
 * the full string has resolved.
 */
@Composable
fun DecryptText(
    target: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 40.sp,
    color: Color = Color.White,
    onComplete: () -> Unit = {}
) {
    val scramble = remember {
        ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#%&@\$!?/<>*+=" +
            "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾊﾋﾌﾍﾎ").toList()
    }
    var display by remember(target) { mutableStateOf(" ".repeat(target.length)) }

    LaunchedEffect(target) {
        val n = target.length
        val framesPerChar = 4          // how long each char stays scrambling before it locks
        val tail = 6                   // extra frames so the last char shimmers briefly
        val totalFrames = n * framesPerChar + tail
        for (frame in 0..totalFrames) {
            val locked = frame / framesPerChar
            val sb = StringBuilder(n)
            for (i in 0 until n) {
                val c = target[i]
                when {
                    c == ' ' -> sb.append(' ')
                    i < locked -> sb.append(c)
                    else -> sb.append(scramble[Random.nextInt(scramble.size)])
                }
            }
            display = sb.toString()
            delay(45)
        }
        display = target
        onComplete()
    }

    Text(
        text = display,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center,
            shadow = Shadow(
                color = Color.White.copy(alpha = 0.65f),
                offset = Offset(0f, 0f),
                blurRadius = 28f
            )
        )
    )
}
