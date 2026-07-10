package com.truelokal.flashforge.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truelokal.flashforge.service.FlashlightController
import com.truelokal.flashforge.service.MorseSignalDecoder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The flash pattern mode. */
enum class StrobeMode {
    OFF, STROBE, SOS
}

/**
 * Complete UI state for the flashlight screen.
 */
data class FlashlightUiState(
    val isOn: Boolean = false,
    val intensity: Float = 1f,            // 0.0–1.0 normalized
    val maxLevel: Int = 1,
    val supportsIntensity: Boolean = false,
    val hasFlash: Boolean = true,
    val strobeMode: StrobeMode = StrobeMode.OFF,
    val isStrobeActive: Boolean = false,
    // Morse code transmission state
    val isMorseSending: Boolean = false,
    val morseCurrentChar: Char? = null,
    val morseProgress: Float = 0f,
    val morsePreview: String = "",
    // Morse code decoding state
    val isDecoding: Boolean = false,
    val decodedText: String = "",
    val decodedSignals: String = "",
    val liveLuminance: Double = 0.0,
    val isLumaActive: Boolean = false,
    // Onboarding state
    val isOnboardingCompleted: Boolean = false,
)

/**
 * ViewModel managing flashlight state, intensity, strobe/SOS patterns,
 * Morse transmission, and camera-based Morse decoding.
 */
class FlashlightViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = FlashlightController(application.applicationContext)
    private val prefs = application.getSharedPreferences("flashforge_prefs", Context.MODE_PRIVATE)

    private val _strobeMode = MutableStateFlow(StrobeMode.OFF)
    private val _intensity = MutableStateFlow(1f)
    private var strobeJob: Job? = null

    // Morse code transmission state
    private val _isMorseSending = MutableStateFlow(false)
    private val _morseCurrentChar = MutableStateFlow<Char?>(null)
    private val _morseProgress = MutableStateFlow(0f)
    private val _morsePreview = MutableStateFlow("")
    private var morseJob: Job? = null

    // Morse code decoding state
    private val _isDecoding = MutableStateFlow(false)
    private val _decodedText = MutableStateFlow("")
    private val _decodedSignals = MutableStateFlow("")
    private val _liveLuminance = MutableStateFlow(0.0)
    private val _isLumaActive = MutableStateFlow(false)

    private val _isOnboardingCompleted =
        MutableStateFlow(prefs.getBoolean("onboarding_complete", false))

    private val morseDecoder = MorseSignalDecoder { text, signals ->
        _decodedText.value = text
        _decodedSignals.value = signals
    }

    private val _uiState = MutableStateFlow(
        FlashlightUiState(
            hasFlash = controller.hasFlash,
            supportsIntensity = controller.supportsIntensity,
            maxLevel = controller.maxStrengthLevel,
            intensity = if (controller.supportsIntensity) {
                controller.defaultStrengthLevel.toFloat() / controller.maxStrengthLevel.toFloat()
            } else {
                1f
            },
            isOnboardingCompleted = _isOnboardingCompleted.value
        )
    )
    val uiState: StateFlow<FlashlightUiState> = _uiState.asStateFlow()

    init {
        // Combined observer for all states
        viewModelScope.launch {
            combine<Any?, FlashlightUiState>(
                listOf(
                    controller.isTorchOn,
                    controller.currentStrength,
                    _strobeMode,
                    _intensity,
                    _isMorseSending,
                    _morseCurrentChar,
                    _morseProgress,
                    _morsePreview,
                    _isDecoding,
                    _decodedText,
                    _decodedSignals,
                    _liveLuminance,
                    _isLumaActive,
                    _isOnboardingCompleted
                )
            ) { array ->
                val isOn = array[0] as Boolean
                val strength = array[1] as Int
                val strobeMode = array[2] as StrobeMode
                val intensity = array[3] as Float
                val isMorseSending = array[4] as Boolean
                val morseCurrentChar = array[5] as Char?
                val morseProgress = array[6] as Float
                val morsePreview = array[7] as String
                val isDecoding = array[8] as Boolean
                val decodedText = array[9] as String
                val decodedSignals = array[10] as String
                val liveLuminance = array[11] as Double
                val isLumaActive = array[12] as Boolean
                val isOnboardingCompleted = array[13] as Boolean

                FlashlightUiState(
                    isOn = isOn,
                    intensity = if (controller.supportsIntensity && controller.maxStrengthLevel > 1) {
                        if (strength > 0) {
                            strength.toFloat() / controller.maxStrengthLevel.toFloat()
                        } else {
                            intensity
                        }
                    } else {
                        intensity
                    },
                    maxLevel = controller.maxStrengthLevel,
                    supportsIntensity = controller.supportsIntensity,
                    hasFlash = controller.hasFlash,
                    strobeMode = strobeMode,
                    isStrobeActive = strobeMode != StrobeMode.OFF && isOn,
                    isMorseSending = isMorseSending,
                    morseCurrentChar = morseCurrentChar,
                    morseProgress = morseProgress,
                    morsePreview = morsePreview,
                    isDecoding = isDecoding,
                    decodedText = decodedText,
                    decodedSignals = decodedSignals,
                    liveLuminance = liveLuminance,
                    isLumaActive = isLumaActive,
                    isOnboardingCompleted = isOnboardingCompleted
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    /**
     * Mark onboarding complete.
     */
    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        _isOnboardingCompleted.value = true
    }

    /**
     * Start Morse decoding from live camera stream.
     */
    fun startDecoding() {
        // Stop any flash active states
        stopStrobe()
        stopMorse()

        _decodedText.value = ""
        _decodedSignals.value = ""
        _liveLuminance.value = 0.0
        _isLumaActive.value = false
        morseDecoder.clear()

        _isDecoding.value = true
    }

    /**
     * Stop Morse decoding.
     */
    fun stopDecoding() {
        _isDecoding.value = false
        _isLumaActive.value = false
        morseDecoder.clear()
    }

    /**
     * Process live frame luminance and light-on state from CameraX analyzer.
     */
    fun onLiveLuminanceChanged(luma: Double, isLightOn: Boolean) {
        if (!_isDecoding.value) return
        _liveLuminance.value = luma
        _isLumaActive.value = isLightOn
        morseDecoder.onLightStateChanged(isLightOn)
    }

    /**
     * Clear currently decoded text.
     */
    fun clearDecodedText() {
        morseDecoder.clear()
        _decodedText.value = ""
        _decodedSignals.value = ""
    }

    /**
     * Toggle flashlight state.
     */
    fun toggleFlashlight() {
        if (!controller.hasFlash) return

        if (_strobeMode.value != StrobeMode.OFF) {
            stopStrobe()
        }

        if (_isMorseSending.value) {
            stopMorse()
        }

        if (_isDecoding.value) {
            stopDecoding()
        }

        if (controller.supportsIntensity && !controller.isTorchOn.value) {
            val level = (_intensity.value * controller.maxStrengthLevel).toInt()
                .coerceIn(1, controller.maxStrengthLevel)
            controller.setStrength(level)
        } else {
            controller.toggleTorch()
        }
    }

    /**
     * Set flashlight intensity.
     */
    fun setIntensity(normalizedIntensity: Float) {
        if (!controller.supportsIntensity) return
        val clamped = normalizedIntensity.coerceIn(0.05f, 1f)
        _intensity.value = clamped

        if (controller.isTorchOn.value) {
            val level = (clamped * controller.maxStrengthLevel).toInt()
                .coerceIn(1, controller.maxStrengthLevel)
            controller.setStrength(level)
        }
    }

    /**
     * Set strobe mode.
     */
    fun setStrobeMode(mode: StrobeMode) {
        if (_isMorseSending.value) {
            stopMorse()
        }
        if (_isDecoding.value) {
            stopDecoding()
        }

        _strobeMode.value = mode

        when (mode) {
            StrobeMode.OFF -> stopStrobe()
            StrobeMode.STROBE -> startStrobe()
            StrobeMode.SOS -> startSOS()
        }
    }

    /**
     * Send Morse code text message.
     */
    fun sendMorseMessage(message: String) {
        if (!controller.hasFlash || message.isBlank()) return
        stopStrobe()
        stopMorse()
        if (_isDecoding.value) {
            stopDecoding()
        }

        val text = message.uppercase().trim()
        val morseChars = text.map { char ->
            char to (MORSE_CODE_MAP[char] ?: "")
        }

        _morsePreview.value = morseChars.joinToString("  ") { (char, morse) ->
            if (char == ' ') " / " else morse
        }

        val totalChars = text.length

        morseJob = viewModelScope.launch {
            _isMorseSending.value = true

            for ((index, pair) in morseChars.withIndex()) {
                if (!isActive) break
                val (char, morse) = pair
                _morseCurrentChar.value = char
                _morseProgress.value = index.toFloat() / totalChars.toFloat()

                if (char == ' ') {
                    delay(UNIT_MS * 7)
                    continue
                }
                if (morse.isEmpty()) continue

                for ((signalIndex, signal) in morse.withIndex()) {
                    if (!isActive) break
                    when (signal) {
                        '·', '.' -> {
                            controller.turnOn()
                            delay(UNIT_MS)
                            controller.turnOff()
                        }

                        '—', '-' -> {
                            controller.turnOn()
                            delay(UNIT_MS * 3)
                            controller.turnOff()
                        }
                    }
                    if (signalIndex < morse.length - 1) {
                        delay(UNIT_MS)
                    }
                }

                if (index < morseChars.size - 1) {
                    val nextChar = morseChars[index + 1].first
                    if (nextChar != ' ') {
                        delay(UNIT_MS * 3)
                    }
                }
            }

            _morseProgress.value = 1f
            delay(300)
            resetMorseState()
        }
    }

    /**
     * Stop Morse transmission.
     */
    fun stopMorse() {
        morseJob?.cancel()
        morseJob = null
        if (controller.isTorchOn.value) {
            controller.turnOff()
        }
        resetMorseState()
    }

    private fun resetMorseState() {
        _isMorseSending.value = false
        _morseCurrentChar.value = null
        _morseProgress.value = 0f
        _morsePreview.value = ""
    }

    private fun startStrobe() {
        stopStrobe()
        strobeJob = viewModelScope.launch {
            while (isActive) {
                controller.turnOn()
                delay(80)
                controller.turnOff()
                delay(80)
            }
        }
    }

    private fun startSOS() {
        stopStrobe()
        strobeJob = viewModelScope.launch {
            while (isActive) {
                repeat(3) {
                    controller.turnOn()
                    delay(150)
                    controller.turnOff()
                    delay(150)
                }
                delay(200)
                repeat(3) {
                    controller.turnOn()
                    delay(450)
                    controller.turnOff()
                    delay(150)
                }
                delay(200)
                repeat(3) {
                    controller.turnOn()
                    delay(150)
                    controller.turnOff()
                    delay(150)
                }
                delay(800)
            }
        }
    }

    private fun stopStrobe() {
        strobeJob?.cancel()
        strobeJob = null
        _strobeMode.value = StrobeMode.OFF
        if (controller.isTorchOn.value) {
            controller.turnOff()
        }
    }

    fun onPause() {
        stopStrobe()
        stopMorse()
        stopDecoding()
    }

    override fun onCleared() {
        super.onCleared()
        stopStrobe()
        stopMorse()
        stopDecoding()
        controller.release()
    }

    companion object {
        private const val UNIT_MS = 200L
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
    }
}
