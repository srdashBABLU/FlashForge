package com.truelokal.flashforge.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.truelokal.flashforge.ui.theme.AccentAmber
import com.truelokal.flashforge.ui.theme.SOSColor
import com.truelokal.flashforge.ui.theme.SuccessGreen

/**
 * Animated status indicator showing the current flashlight state.
 *
 * States: "Ready", "On", "No Flash Available"
 * Transitions smoothly between states with slide + fade animations.
 */
@Composable
fun StatusIndicator(
    isOn: Boolean,
    hasFlash: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText: String
    val statusColor: Color
    val statusIcon = when {
        !hasFlash -> {
            statusText = "No flashlight available"
            statusColor = SOSColor
            Icons.Rounded.Warning
        }
        isOn -> {
            statusText = "Flashlight On"
            statusColor = AccentAmber
            Icons.Rounded.FlashOn
        }
        else -> {
            statusText = "Ready"
            statusColor = SuccessGreen
            Icons.Rounded.Check
        }
    }

    AnimatedContent(
        targetState = statusText,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = slideInVertically(tween(300)) { -it } + fadeIn(tween(300)),
                initialContentExit = slideOutVertically(tween(300)) { it } + fadeOut(tween(200)),
            )
        },
        label = "statusTransition",
        modifier = modifier,
    ) { text ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = statusColor,
            )
        }
    }
}
