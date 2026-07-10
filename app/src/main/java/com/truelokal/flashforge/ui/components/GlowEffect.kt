package com.truelokal.flashforge.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.truelokal.flashforge.ui.theme.GlowAmberCenter
import com.truelokal.flashforge.ui.theme.GlowAmberEdge

/**
 * Full-screen radial gradient glow effect that responds to flashlight state.
 *
 * When the flashlight is on, an ambient amber glow radiates from the center,
 * with intensity and radius animated based on the brightness level.
 * The glow gently pulses to create a "living light" effect.
 */
@Composable
fun GlowEffect(
    isOn: Boolean,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    // Animated alpha based on on/off state
    val targetAlpha by animateFloatAsState(
        targetValue = if (isOn) intensity.coerceIn(0.3f, 1f) else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "glowAlpha"
    )

    // Subtle pulse animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (targetAlpha > 0.01f) {
            val radius = (size.minDimension * 0.7f) * pulse * intensity

            // Primary amber glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlowAmberCenter.copy(alpha = targetAlpha * 0.7f),
                        GlowAmberCenter.copy(alpha = targetAlpha * 0.3f),
                        GlowAmberEdge,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = radius
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius
            )

            // Secondary soft white glow (inner)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = targetAlpha * 0.15f),
                        Color.White.copy(alpha = targetAlpha * 0.05f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = radius * 0.4f
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius * 0.4f
            )
        }
    }
}

