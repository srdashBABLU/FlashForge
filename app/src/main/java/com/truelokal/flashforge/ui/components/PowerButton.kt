package com.truelokal.flashforge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truelokal.flashforge.ui.theme.AccentAmber
import com.truelokal.flashforge.ui.theme.AccentGlow
import com.truelokal.flashforge.ui.theme.FlashOffColor
import com.truelokal.flashforge.ui.theme.FlashOffColorLight
import com.truelokal.flashforge.ui.theme.FlashOnColor

/**
 * Large animated power button for flashlight toggle.
 *
 * Features:
 * - Smooth color morph between off (gray) and on (amber)
 * - Scale bounce on press
 * - Pulsing outer ring glow when active
 * - Haptic feedback on toggle
 */
@Composable
fun PowerButton(
    isOn: Boolean,
    enabled: Boolean = true,
    size: Dp = 140.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animated scale on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "buttonScale"
    )

    // Animated colors
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val buttonColor by animateColorAsState(
        targetValue = if (isOn) FlashOnColor else if (isDark) FlashOffColor else FlashOffColorLight,
        animationSpec = tween(400),
        label = "buttonColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.7f
        ),
        animationSpec = tween(400),
        label = "iconColor"
    )

    // Pulsing glow ring
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )
    val animatedRingAlpha by animateFloatAsState(
        targetValue = if (isOn) ringAlpha else 0f,
        animationSpec = tween(500),
        label = "ringAlphaSwitch"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .semantics {
                contentDescription = if (isOn) "Turn off flashlight" else "Turn on flashlight"
                role = Role.Button
            }
    ) {
        // Outer glow ring
        Canvas(modifier = Modifier.size(size)) {
            if (animatedRingAlpha > 0.01f) {
                // Outer glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentGlow.copy(alpha = animatedRingAlpha * 0.4f),
                            AccentAmber.copy(alpha = animatedRingAlpha * 0.2f),
                            Color.Transparent,
                        ),
                        radius = this.size.minDimension / 2f * 1.3f,
                    ),
                    radius = this.size.minDimension / 2f * 1.3f,
                )

                // Ring stroke
                drawCircle(
                    color = AccentAmber.copy(alpha = animatedRingAlpha * 0.6f),
                    radius = this.size.minDimension / 2f - 4.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }

        // Button background
        Canvas(
            modifier = Modifier
                .size(size - 24.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                )
        ) {
            // Main circle fill
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        buttonColor.copy(alpha = 0.95f),
                        buttonColor.copy(alpha = 0.8f),
                    ),
                    center = Offset(
                        this.size.width * 0.45f,
                        this.size.height * 0.4f
                    ),
                ),
            )

            // Subtle highlight on top
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isOn) 0.2f else 0.1f),
                        Color.Transparent,
                    ),
                    start = Offset(this.size.width * 0.3f, 0f),
                    end = Offset(this.size.width * 0.7f, this.size.height),
                ),
            )
        }

        // Power icon
        Icon(
            imageVector = Icons.Rounded.PowerSettingsNew,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.35f)
        )
    }
}

/**
 * Extension to compute luminance for dark/light detection.
 */
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
