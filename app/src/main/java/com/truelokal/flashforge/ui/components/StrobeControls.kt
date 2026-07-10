package com.truelokal.flashforge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.truelokal.flashforge.ui.theme.SOSColor
import com.truelokal.flashforge.ui.theme.StrobeColor
import com.truelokal.flashforge.viewmodel.StrobeMode

/**
 * Row of selectable mode chips: Off, Strobe, SOS.
 *
 * Each chip shows an icon and label, with animated selection state.
 */
@Composable
fun StrobeControls(
    currentMode: StrobeMode,
    onModeSelected: (StrobeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = androidx.compose.ui.Alignment.CenterHorizontally),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        ModeChip(
            label = "Off",
            icon = Icons.Rounded.FlashOff,
            isSelected = currentMode == StrobeMode.OFF,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onModeSelected(StrobeMode.OFF)
            }
        )
        ModeChip(
            label = "Strobe",
            icon = Icons.Rounded.FlashOn,
            isSelected = currentMode == StrobeMode.STROBE,
            accentColor = StrobeColor,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onModeSelected(StrobeMode.STROBE)
            }
        )
        ModeChip(
            label = "SOS",
            icon = Icons.Rounded.Sos,
            isSelected = currentMode == StrobeMode.SOS,
            accentColor = SOSColor,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onModeSelected(StrobeMode.SOS)
            }
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "chipColor"
    )

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            selectedContainerColor = containerColor,
            selectedLabelColor = accentColor,
            selectedLeadingIconColor = accentColor,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = accentColor.copy(alpha = 0.5f),
            enabled = true,
            selected = isSelected,
        ),
    )
}
