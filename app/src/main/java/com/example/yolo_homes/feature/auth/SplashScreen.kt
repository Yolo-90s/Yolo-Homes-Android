package com.example.yolo_homes.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.yolo_homes.ui.components.DecryptText

/**
 * Futuristic AI boot sequence: pure black, the brand name decrypts from scrambled symbols,
 * then a hairline glow divider and tagline fade in. Cinematic, minimal, center aligned.
 */
@Composable
fun SplashScreen() {
    var decrypted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            DecryptText(
                target = "YOLO HOMES",
                fontSize = 38.sp,
                onComplete = { decrypted = true }
            )

            AnimatedVisibility(visible = decrypted, enter = fadeIn(tween(700))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // hairline glow divider
                    Box(
                        Modifier
                            .padding(bottom = 14.dp)
                            .height(1.dp)
                            .width(150.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.7f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Text(
                        text = "SMART APARTMENT MANAGEMENT",
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
