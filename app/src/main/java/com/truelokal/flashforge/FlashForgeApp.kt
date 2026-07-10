package com.truelokal.flashforge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truelokal.flashforge.ui.screens.FlashlightScreen
import com.truelokal.flashforge.ui.screens.OnboardingScreen
import com.truelokal.flashforge.ui.theme.FlashForgeTheme
import com.truelokal.flashforge.viewmodel.FlashlightViewModel

/**
 * Root composable for FlashForge.
 *
 * Checks onboarding status:
 * - If onboarding is NOT completed, displays the multipage OnboardingScreen.
 * - Otherwise, displays the main FlashlightScreen with all states.
 */
@Composable
fun FlashForgeApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val viewModel: FlashlightViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FlashForgeTheme(darkTheme = isDarkTheme) {
        if (!uiState.isOnboardingCompleted) {
            OnboardingScreen(
                onFinished = {
                    viewModel.completeOnboarding()
                }
            )
        } else {
            FlashlightScreen(
                uiState = uiState,
                isDarkTheme = isDarkTheme,
                onToggle = viewModel::toggleFlashlight,
                onIntensityChange = viewModel::setIntensity,
                onStrobeModeChange = viewModel::setStrobeMode,
                onSendMorseMessage = viewModel::sendMorseMessage,
                onStopMorse = viewModel::stopMorse,
                onStartDecoding = viewModel::startDecoding,
                onStopDecoding = viewModel::stopDecoding,
                onLiveLuminanceChanged = viewModel::onLiveLuminanceChanged,
                onClearDecodedText = viewModel::clearDecodedText,
                onToggleTheme = onToggleTheme,
            )
        }
    }
}
