package com.truelokal.flashforge.service

/**
 * Decodes light state intervals into Morse signals (dots, dashes, spaces)
 * and translates them to English text in real-time.
 *
 * Implements a dynamic WPM (speed) tracking engine that adaptively calibrates
 * the unit duration based on incoming signals. This makes it highly accurate
 * on low-end devices and resilient to frame rate fluctuations or variable speeds.
 */
class MorseSignalDecoder(
    private val onTextDecoded: (String, String) -> Unit
) {
    private var lastState = false
    private var lastStateChangeTime = System.currentTimeMillis()
    private val signalBuffer = StringBuilder()
    private val textBuffer = StringBuilder()

    // Dynamically calibrated base unit length (typically 120-250ms; starts at 150ms)
    private var unitMs = 150.0

    /**
     * Feed the live detected state of light.
     */
    fun onLightStateChanged(isLightOn: Boolean) {
        val now = System.currentTimeMillis()
        val duration = now - lastStateChangeTime

        if (isLightOn != lastState) {
            // Debounce transient spikes (reject pulses/gaps under 45ms as camera frame jitter)
            if (duration < 45) {
                return
            }

            if (lastState) {
                // Light was ON: classify as Dot vs Dash based on current unitMs estimate
                val unitThreshold = 2.0 * unitMs
                if (duration < unitThreshold) {
                    signalBuffer.append("·")
                    // Adapt unitMs towards this dot duration (dot = 1 unit)
                    unitMs = (unitMs * 0.82) + (duration * 0.18)
                } else {
                    signalBuffer.append("—")
                    // Adapt unitMs based on this dash duration (dash = 3 units)
                    val estimatedUnit = duration / 3.0
                    unitMs = (unitMs * 0.82) + (estimatedUnit * 0.18)
                }
                // Cap unit duration to reasonable ranges (50ms to 450ms) to prevent drift errors
                unitMs = unitMs.coerceIn(60.0, 400.0)
            } else {
                // Light was OFF: classify gap durations to split characters or words
                val charThreshold = 2.4 * unitMs
                val wordThreshold = 5.2 * unitMs

                if (duration >= wordThreshold) {
                    // Word gap: finalize current character, then insert space
                    decodeCurrentCharacter()
                    if (textBuffer.isNotEmpty() && !textBuffer.endsWith(" ")) {
                        textBuffer.append(" ")
                    }
                    // Adapt unitMs based on word gap (word gap = 7 units)
                    val estimatedUnit = duration / 7.0
                    if (estimatedUnit < 400.0) {
                        unitMs = (unitMs * 0.9) + (estimatedUnit * 0.1)
                    }
                } else if (duration >= charThreshold) {
                    // Character gap: finalize current character
                    decodeCurrentCharacter()
                    // Adapt unitMs based on character gap (char gap = 3 units)
                    val estimatedUnit = duration / 3.0
                    unitMs = (unitMs * 0.9) + (estimatedUnit * 0.1)
                } else {
                    // Part gap (between elements of same char, e.g. dot gap = 1 unit)
                    unitMs = (unitMs * 0.92) + (duration * 0.08)
                }
                unitMs = unitMs.coerceIn(60.0, 400.0)
            }

            lastState = isLightOn
            lastStateChangeTime = now
            triggerCallback()
        } else {
            // Auto-flush: if light has been OFF for a long time, finalize the current letter
            val flushThreshold = maxOf(2000.0, 8.0 * unitMs)
            if (!isLightOn && duration > flushThreshold && signalBuffer.isNotEmpty()) {
                decodeCurrentCharacter()
                triggerCallback()
            }
        }
    }

    private fun decodeCurrentCharacter() {
        val rawChar = signalBuffer.toString()
        if (rawChar.isNotEmpty()) {
            val letter = REVERSE_MORSE_CODE_MAP[rawChar] ?: "?"
            textBuffer.append(letter)
            signalBuffer.clear()
        }
    }

    private fun triggerCallback() {
        onTextDecoded(textBuffer.toString(), signalBuffer.toString())
    }

    /**
     * Clear all buffers to start a new decoding session.
     */
    fun clear() {
        signalBuffer.clear()
        textBuffer.clear()
        lastState = false
        lastStateChangeTime = System.currentTimeMillis()
        unitMs = 150.0 // Reset calibration
        triggerCallback()
    }

    companion object {
        private val REVERSE_MORSE_CODE_MAP = mapOf(
            "·—" to "A", "—···" to "B", "—·—·" to "C", "—··" to "D",
            "·" to "E", "··—·" to "F", "——·" to "G", "····" to "H",
            "··" to "I", "·———" to "J", "—·—" to "K", "·—··" to "L",
            "——" to "M", "—·" to "N", "———" to "O", "·——·" to "P",
            "——·—" to "Q", "·—·" to "R", "···" to "S", "—" to "T",
            "··—" to "U", "···—" to "V", "·——" to "W", "—··—" to "X",
            "—·——" to "Y", "——··" to "Z",
            "—————" to "0", "·————" to "1", "··———" to "2", "···——" to "3",
            "····—" to "4", "·····" to "5", "—····" to "6", "——···" to "7",
            "———··" to "8", "————·" to "9",
            "·—·—·—" to ".", "——··——" to ",", "··——··" to "?",
            "—·—·——" to "!", "—··—·" to "/", "—·——·" to "(",
            "—·——·—" to ")", "·—···" to "&", "———···" to ":",
            "—·—·—·" to ";", "—···—" to "=", "·—·—·" to "+",
            "—····—" to "-", "··——·—" to "_", "·—··—·" to "\"",
            "·————·" to "'", "·——·—·" to "@"
        )
    }
}
