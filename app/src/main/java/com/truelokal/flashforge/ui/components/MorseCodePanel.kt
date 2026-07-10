package com.truelokal.flashforge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truelokal.flashforge.ui.theme.AccentAmber
import com.truelokal.flashforge.ui.theme.SOSColor
import com.truelokal.flashforge.ui.theme.SuccessGreen


/**
 * Morse code message input panel.
 *
 * Allows users to type a text message, see its Morse code translation
 * in real-time, and transmit it as flash signals. Shows progress and
 * the currently transmitting character during playback.
 */
@Composable
fun MorseCodePanel(
    isSending: Boolean,
    currentChar: Char?,
    morseProgress: Float,
    morsePreview: String,
    onSendMessage: (String) -> Unit,
    onStopMorse: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var messageText by rememberSaveable { mutableStateOf("") }


    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 8.dp else 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 12.dp else 16.dp,
                vertical = if (isCompact) 10.dp else 14.dp
            )
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "📡",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Morse Code Message",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

            // Input field + Send button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = {
                        Text(
                            text = "Type your message...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    enabled = !isSending,
                    singleLine = false,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) AccentAmber else Color(0xFF9E6400),
                        cursorColor = if (isDark) AccentAmber else Color(0xFF9E6400),
                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.weight(1f),
                )

                if (isSending) {
                    // Stop button
                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStopMorse()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = SOSColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Stop sending",
                        )
                    }
                } else {
                    // Send button
                    FilledIconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                keyboardController?.hide()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSendMessage(messageText.trim())
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AccentAmber,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send as Morse code",
                        )
                    }
                }
            }

            // Morse code preview
            AnimatedVisibility(
                visible = messageText.isNotBlank() || isSending,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    val displayMorse = if (isSending && morsePreview.isNotBlank()) {

                        morsePreview
                    } else {
                        textToMorsePreview(messageText)
                    }

                    val clipboardManager = LocalClipboardManager.current
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = displayMorse,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = if (isSending) {
                                if (isDark) AccentAmber else Color(0xFF9E6400)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 32.dp) // Leave room for copy button
                        )
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(AnnotatedString(displayMorse))
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy Morse code",
                                tint = if (isDark) AccentAmber else Color(0xFF9E6400),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }



            // Transmission progress
            AnimatedVisibility(
                visible = isSending,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Pulsing indicator dot
                        val pulse = rememberInfiniteTransition(label = "morsePulse")
                        val dotAlpha by pulse.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "dotPulse"
                        )

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentAmber.copy(alpha = dotAlpha))
                        )

                        Text(
                            text = if (currentChar != null) {
                                if (currentChar == ' ') "Sending: [space]"
                                else "Sending: '${currentChar.uppercaseChar()}'"
                            } else {
                                "Preparing..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "${(morseProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { morseProgress },
                        color = AccentAmber,
                        trackColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}

/**
 * Quick preview of text to Morse code translation (for the input field).
 */
private fun textToMorsePreview(text: String): String {
    if (text.isBlank()) return ""
    return text.uppercase().map { char ->
        MORSE_CODE_MAP[char] ?: if (char == ' ') " / " else "?"
    }.joinToString("  ")
}

/** Standard International Morse Code mapping. */
private val MORSE_CODE_MAP = mapOf(
    'A' to "·—", 'B' to "—···", 'C' to "—·—·", 'D' to "—··",
    'E' to "·", 'F' to "··—·", 'G' to "——·", 'H' to "····",
    'I' to "··", 'J' to "·———", 'K' to "—·—", 'L' to "·—··",
    'M' to "——", 'N' to "—·", 'O' to "———", 'P' to "·——·",
    'Q' to "——·—", 'R' to "·—·", 'S' to "···", 'T' to "—",
    'U' to "··—", 'V' to "···—", 'W' to "·——", 'X' to "—··—",
    'Y' to "—·——", 'Z' to "——··",
    '0' to "—————", '1' to "·————", '2' to "··———", '3' to "···——",
    '4' to "····—", '5' to "·····", '6' to "—····", '7' to "——···",
    '8' to "———··", '9' to "————·",
    '.' to "·—·—·—", ',' to "——··——", '?' to "··——··",
    '!' to "—·—·——", '/' to "—··—·", '(' to "—·——·",
    ')' to "—·——·—", '&' to "·—···", ':' to "———···",
    ';' to "—·—·—·", '=' to "—···—", '+' to "·—·—·",
    '-' to "—····—", '_' to "··——·—", '"' to "·—··—·",
    '\'' to "·————·", '@' to "·——·—·",
)
