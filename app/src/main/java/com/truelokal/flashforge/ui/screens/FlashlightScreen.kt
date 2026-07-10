package com.truelokal.flashforge.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.truelokal.flashforge.ui.components.CameraPermissionDialog
import com.truelokal.flashforge.ui.components.CameraPreviewView
import com.truelokal.flashforge.ui.components.GlowEffect
import com.truelokal.flashforge.ui.components.IntensityDial
import com.truelokal.flashforge.ui.components.MorseCodePanel
import com.truelokal.flashforge.ui.components.PowerButton
import com.truelokal.flashforge.ui.components.StatusIndicator
import com.truelokal.flashforge.ui.components.StrobeControls
import com.truelokal.flashforge.ui.theme.AccentAmber
import com.truelokal.flashforge.ui.theme.SOSColor
import com.truelokal.flashforge.viewmodel.FlashlightUiState
import com.truelokal.flashforge.viewmodel.StrobeMode

/**
 * Main flashlight screen composable.
 *
 * Provides a tab bar to switch between:
 * 0. Flashlight controller view
 * 1. Morse Decoder camera analysis view
 */
@Composable
fun FlashlightScreen(
    uiState: FlashlightUiState,
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    onIntensityChange: (Float) -> Unit,
    onStrobeModeChange: (StrobeMode) -> Unit,
    onSendMorseMessage: (String) -> Unit,
    onStopMorse: () -> Unit,
    onStartDecoding: () -> Unit,
    onStopDecoding: () -> Unit,
    onLiveLuminanceChanged: (Double, Boolean) -> Unit,
    onClearDecodedText: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showPermissionExplanation by remember { mutableStateOf(false) }

    // Launcher for system permission dialog
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartDecoding()
        } else {
            onStopDecoding()
            selectedTab = 0 // Go back to controller
        }
    }

    val requestCameraPermission = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onStartDecoding()
        } else {
            showPermissionExplanation = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )

            // Premium custom TabRow
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentAmber,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onStopDecoding()
                    },
                    text = {
                        Text(
                            text = "Flashlight",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        requestCameraPermission()
                    },
                    text = {
                        Text(
                            text = "Morse Decoder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                    if (isLandscape) {
                        LandscapeLayout(
                            uiState = uiState,
                            onToggle = onToggle,
                            onIntensityChange = onIntensityChange,
                            onStrobeModeChange = onStrobeModeChange,
                            onSendMorseMessage = onSendMorseMessage,
                            onStopMorse = onStopMorse,
                            isDarkTheme = isDarkTheme,
                        )
                    } else {
                        PortraitLayout(
                            uiState = uiState,
                            onToggle = onToggle,
                            onIntensityChange = onIntensityChange,
                            onStrobeModeChange = onStrobeModeChange,
                            onSendMorseMessage = onSendMorseMessage,
                            onStopMorse = onStopMorse,
                            isDarkTheme = isDarkTheme,
                        )
                    }
                } else {
                    // Decoder Tab
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasCameraPermission && uiState.isDecoding) {
                        MorseDecoderLayout(
                            uiState = uiState,
                            onLiveLuminanceChanged = onLiveLuminanceChanged,
                            onClearDecodedText = onClearDecodedText,
                            onStopDecoding = {
                                onStopDecoding()
                                selectedTab = 0
                            },
                            isDark = isDarkTheme
                        )
                    } else {
                        // View for requesting permission / empty state
                        DecoderPlaceholder(
                            onRequestPermission = { requestCameraPermission() },
                            isDark = isDarkTheme
                        )
                    }
                }
            }
        }

        // Custom camera permission modal overlay
        if (showPermissionExplanation) {
            CameraPermissionDialog(
                onDismiss = {
                    showPermissionExplanation = false
                    selectedTab = 0
                },
                onConfirm = {
                    showPermissionExplanation = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Morse Decoder Layout
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MorseDecoderLayout(
    uiState: FlashlightUiState,
    onLiveLuminanceChanged: (Double, Boolean) -> Unit,
    onClearDecodedText: () -> Unit,
    onStopDecoding: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Viewfinder left
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                CameraPreviewView(onLuminanceChanged = onLiveLuminanceChanged)
            }

            // Results right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DecoderStatsCard(
                    isLumaActive = uiState.isLumaActive,
                    liveLuminance = uiState.liveLuminance,
                    isDark = isDark
                )

                DecoderOutputCard(
                    decodedText = uiState.decodedText,
                    decodedSignals = uiState.decodedSignals,
                    onClear = onClearDecodedText,
                    onStop = onStopDecoding,
                    isDark = isDark
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera feed upper half
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                CameraPreviewView(onLuminanceChanged = onLiveLuminanceChanged)
            }

            // Status bar
            DecoderStatsCard(
                isLumaActive = uiState.isLumaActive,
                liveLuminance = uiState.liveLuminance,
                isDark = isDark
            )

            // Results card lower half
            DecoderOutputCard(
                decodedText = uiState.decodedText,
                decodedSignals = uiState.decodedSignals,
                onClear = onClearDecodedText,
                onStop = onStopDecoding,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DecoderStatsCard(
    isLumaActive: Boolean,
    liveLuminance: Double,
    isDark: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x1AFFFFFF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val glowAlpha by pulse.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseGlow"
                )

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLumaActive) AccentAmber.copy(alpha = glowAlpha)
                            else Color.Gray.copy(alpha = 0.5f)
                        )
                )
                Text(
                    text = if (isLumaActive) "Flash Detected!" else "Waiting for Flash...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLumaActive) AccentAmber else (if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                )
            }

            Text(
                text = "Luma: ${liveLuminance.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DecoderOutputCard(
    decodedText: String,
    decodedSignals: String,
    onClear: () -> Unit,
    onStop: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E1E38) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Decoded Output",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (decodedText.isNotEmpty()) {
                    val clipboardManager = LocalClipboardManager.current
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(decodedText))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy decoded text",
                            tint = if (isDark) AccentAmber else Color(0xFF9E6400),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))


            // Main text panel scrollable
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color.Black.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(12.dp)
            ) {
                Text(
                    text = decodedText.ifEmpty { "Position light source in center box to start translating..." },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        lineHeight = 26.sp
                    ),
                    color = if (decodedText.isEmpty()) {
                        if (isDark) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    } else {
                        if (isDark) AccentAmber else Color(0xFF9E6400)
                    },
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }


            // Live signal string buffer underneath
            AnimatedVisibility(
                visible = decodedSignals.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Signals: $decodedSignals",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.weight(1f)
                        )
                        val clipboard = LocalClipboardManager.current
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(decodedSignals))
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy raw signals",
                                tint = if (isDark) AccentAmber.copy(alpha = 0.6f) else Color(0xFF9E6400).copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ClearAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Clear")
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SOSColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Stop")
                }
            }
        }
    }
}

@Composable
private fun DecoderPlaceholder(
    onRequestPermission: () -> Unit,
    isDark: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Camera,
            contentDescription = null,
            tint = if (isDark) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Point your camera at flash sequences to translate them back to text. Permission is required to access frame analyses.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color(0xFF0D0D1A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = "Enable Camera", fontWeight = FontWeight.Bold)
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// Original Portrait Layout
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PortraitLayout(
    uiState: FlashlightUiState,
    onToggle: () -> Unit,
    onIntensityChange: (Float) -> Unit,
    onStrobeModeChange: (StrobeMode) -> Unit,
    onSendMorseMessage: (String) -> Unit,
    onStopMorse: () -> Unit,
    isDarkTheme: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StatusIndicator(
            isOn = uiState.isOn,
            hasFlash = uiState.hasFlash,
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            GlowEffect(
                isOn = uiState.isOn,
                intensity = uiState.intensity,
                modifier = Modifier.fillMaxSize()
            )
            PowerButton(
                isOn = uiState.isOn,
                enabled = uiState.hasFlash && !uiState.isMorseSending,
                size = 150.dp,
                onClick = onToggle,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tap hint
        val hintAlpha by animateFloatAsState(
            targetValue = if (uiState.isOn || uiState.isMorseSending) 0f else 0.6f,
            animationSpec = tween(400),
            label = "hintAlpha"
        )
        Text(
            text = "Tap to toggle",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(hintAlpha),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.supportsIntensity) {
            IntensityDial(
                intensity = uiState.intensity,
                isVisible = true,
                isOn = uiState.isOn,
                onIntensityChange = onIntensityChange,
                size = 220.dp,
            )
        } else {
            IntensityInfoCard(isOn = uiState.isOn)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Strobe controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = "Flash Mode",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            StrobeControls(
                currentMode = uiState.strobeMode,
                onModeSelected = onStrobeModeChange,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Morse code
        MorseCodePanel(
            isSending = uiState.isMorseSending,
            currentChar = uiState.morseCurrentChar,
            morseProgress = uiState.morseProgress,
            morsePreview = uiState.morsePreview,
            onSendMessage = onSendMorseMessage,
            onStopMorse = onStopMorse,
            isDark = isDarkTheme,
        )

        Spacer(modifier = Modifier.height(12.dp))

        CapabilityInfo(
            supportsIntensity = uiState.supportsIntensity,
            maxLevel = uiState.maxLevel,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Original Landscape Layout
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LandscapeLayout(
    uiState: FlashlightUiState,
    onToggle: () -> Unit,
    onIntensityChange: (Float) -> Unit,
    onStrobeModeChange: (StrobeMode) -> Unit,
    onSendMorseMessage: (String) -> Unit,
    onStopMorse: () -> Unit,
    isDarkTheme: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
    ) {
        // Left Column: Power Button + Intensity
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            StatusIndicator(
                isOn = uiState.isOn,
                hasFlash = uiState.hasFlash,
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                GlowEffect(
                    isOn = uiState.isOn,
                    intensity = uiState.intensity,
                    modifier = Modifier.fillMaxSize()
                )
                PowerButton(
                    isOn = uiState.isOn,
                    enabled = uiState.hasFlash && !uiState.isMorseSending,
                    size = 120.dp,
                    onClick = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tap hint
            val hintAlpha by animateFloatAsState(
                targetValue = if (uiState.isOn || uiState.isMorseSending) 0f else 0.6f,
                animationSpec = tween(400),
                label = "hintAlphaLand"
            )
            Text(
                text = "Tap to toggle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(hintAlpha),
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.supportsIntensity) {
                IntensityDial(
                    intensity = uiState.intensity,
                    isVisible = true,
                    isOn = uiState.isOn,
                    onIntensityChange = onIntensityChange,
                    size = 160.dp,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            CapabilityInfo(
                supportsIntensity = uiState.supportsIntensity,
                maxLevel = uiState.maxLevel,
            )
        }

        // Right Column: Modes + Morse
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Flash Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                StrobeControls(
                    currentMode = uiState.strobeMode,
                    onModeSelected = onStrobeModeChange,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            MorseCodePanel(
                isSending = uiState.isMorseSending,
                currentChar = uiState.morseCurrentChar,
                morseProgress = uiState.morseProgress,
                morsePreview = uiState.morsePreview,
                onSendMessage = onSendMorseMessage,
                onStopMorse = onStopMorse,
                isDark = isDarkTheme,
                isCompact = true,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Shared Components
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TopBar(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Flash",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Forge",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AccentAmber,
            )
        }

        IconButton(onClick = onToggleTheme) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IntensityInfoCard(
    isOn: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.padding(horizontal = 32.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Brightness control is not supported on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CapabilityInfo(
    supportsIntensity: Boolean,
    maxLevel: Int,
    modifier: Modifier = Modifier,
) {
    val text = if (supportsIntensity) {
        "$maxLevel brightness levels available"
    } else {
        "Standard flashlight mode"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 32.dp),
    )
}
