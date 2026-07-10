package com.truelokal.flashforge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truelokal.flashforge.ui.theme.AccentAmber
import com.truelokal.flashforge.ui.theme.AccentGlow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular arc slider for controlling flashlight intensity.
 *
 * The arc spans 270° (from 7 o'clock to 5 o'clock position), with a draggable
 * thumb and a center percentage display. Touch anywhere on or near the arc
 * and drag to adjust brightness.
 */
@Composable
fun IntensityDial(
    intensity: Float,
    isVisible: Boolean,
    isOn: Boolean,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticStep by remember { mutableFloatStateOf(-1f) }

    val animatedIntensity by animateFloatAsState(
        targetValue = intensity,
        animationSpec = tween(if (isDragging) 30 else 300),
        label = "dialIntensity"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (isOn) AccentAmber else MaterialTheme.colorScheme.outline
    val thumbColor = if (isOn) AccentGlow else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (isOn) AccentAmber else MaterialTheme.colorScheme.onSurface

    // Arc parameters (in degrees, Android coordinate system)
    val startAngle = 135f   // 7 o'clock position
    val sweepAngle = 270f   // sweeps clockwise to 5 o'clock

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = tween(400)),
        exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size)
        ) {
            Canvas(
                modifier = Modifier
                    .size(size)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val centerX = this.size.width / 2f
                                val centerY = this.size.height / 2f
                                val x = change.position.x - centerX
                                val y = change.position.y - centerY

                                // Calculate angle from center (Android: 0° = 3 o'clock, clockwise)
                                var angle = Math.toDegrees(
                                    atan2(y.toDouble(), x.toDouble())
                                ).toFloat()
                                if (angle < 0) angle += 360f

                                // Map angle to intensity (0..1)
                                // Our arc starts at 135° and sweeps 270° clockwise
                                // So the arc covers: 135° → 405° (i.e., 135° → 360° → 45°)
                                var relativeAngle = angle - startAngle
                                if (relativeAngle < 0) relativeAngle += 360f

                                // If touch is in the gap (270°..360° relative → 45°..135° actual),
                                // clamp to nearest end
                                val newIntensity = if (relativeAngle <= sweepAngle) {
                                    (relativeAngle / sweepAngle).coerceIn(0.05f, 1f)
                                } else {
                                    // In the gap — snap to closest end
                                    val distToStart = relativeAngle - sweepAngle
                                    val distToEnd = 360f - relativeAngle
                                    if (distToEnd < distToStart) 0.05f else 1f
                                }

                                onIntensityChange(newIntensity)

                                // Haptic at 10% steps
                                val step = (newIntensity * 10).toInt().toFloat()
                                if (step != lastHapticStep) {
                                    lastHapticStep = step
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            ) {
                val strokeWidth = 12.dp.toPx()
                val thumbRadius = 14.dp.toPx()
                val padding = thumbRadius + 4.dp.toPx()
                val arcRadius = (this.size.minDimension / 2f) - padding
                val centerX = this.size.width / 2f
                val centerY = this.size.height / 2f

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                )

                // Active arc
                val activeSweep = sweepAngle * animatedIntensity
                drawArc(
                    color = activeColor,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                )

                // Tick marks
                val tickCount = 10
                for (i in 0..tickCount) {
                    val tickAngle = startAngle + (sweepAngle * i / tickCount)
                    val tickRad = Math.toRadians(tickAngle.toDouble())
                    val innerR = arcRadius - strokeWidth - 4.dp.toPx()
                    val outerR = arcRadius - strokeWidth - 10.dp.toPx()
                    val isMajor = i % 5 == 0

                    drawLine(
                        color = if (i.toFloat() / tickCount <= animatedIntensity) activeColor.copy(alpha = 0.6f)
                        else trackColor.copy(alpha = 0.4f),
                        start = Offset(
                            centerX + innerR * cos(tickRad).toFloat(),
                            centerY + innerR * sin(tickRad).toFloat()
                        ),
                        end = Offset(
                            centerX + outerR * cos(tickRad).toFloat(),
                            centerY + outerR * sin(tickRad).toFloat()
                        ),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // Thumb position
                val thumbAngle = Math.toRadians((startAngle + activeSweep).toDouble())
                val thumbX = centerX + arcRadius * cos(thumbAngle).toFloat()
                val thumbY = centerY + arcRadius * sin(thumbAngle).toFloat()

                // Thumb glow (when on)
                if (isOn) {
                    drawCircle(
                        color = AccentAmber.copy(alpha = 0.25f),
                        radius = thumbRadius * 2f,
                        center = Offset(thumbX, thumbY),
                    )
                }

                // Thumb outer
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(thumbX, thumbY),
                )
                // Thumb inner dot
                drawCircle(
                    color = if (isOn) AccentAmber else trackColor,
                    radius = thumbRadius * 0.45f,
                    center = Offset(thumbX, thumbY),
                )
            }

            // Center percentage label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedIntensity * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                Text(
                    text = "Brightness",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
