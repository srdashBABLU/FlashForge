package com.truelokal.flashforge.service

/**
 * Decodes light state intervals into Morse signals (dots, dashes, spaces)
 * and translates them to English text in real-time.
 *
 * Implements a full-sequence parser rather than immediate mutations.
 * It accumulates the entire stream of raw Morse signals and translates it as a whole
 * to avoid characters getting split by camera exposure lag or transient frame drops.
 */
class MorseSignalDecoder(
    private val onTextDecoded: (String, String) -> Unit
) {
    private var lastState = false
    private var lastStateChangeTime = System.currentTimeMillis()

    // Accumulates the entire raw Morse signal string (e.g. "··· ——— ··· / ·— / ·—·· ·—·· ·—")
    private val signalBuffer = StringBuilder()

    // Dynamically calibrated base unit length (starts at 200ms to align with transmitter)
    private var unitMs = 200.0

    /**
     * Feed the live detected state of light.
     */
    fun onLightStateChanged(isLightOn: Boolean) {
        val now = System.currentTimeMillis()
        val duration = now - lastStateChangeTime

        if (isLightOn != lastState) {
            // Debounce transient noise (reject pulses/gaps under 50ms as camera jitter)
            if (duration < 50) {
                return
            }

            if (lastState) {
                // Light was ON: classify as Dot vs Dash based on unitMs
                val dotDashThreshold = 2.2 * unitMs
                if (duration < dotDashThreshold) {
                    appendSignal("·")
                    // Calibrate unitMs towards dot duration (1 unit)
                    unitMs = (unitMs * 0.85) + (duration * 0.15)
                } else {
                    appendSignal("—")
                    // Calibrate unitMs towards dash duration (3 units)
                    val estimatedUnit = duration / 3.0
                    unitMs = (unitMs * 0.85) + (estimatedUnit * 0.15)
                }
            } else {
                // Light was OFF: classify gaps to separate letters or words
                val charThreshold = 2.6 * unitMs
                val wordThreshold = 5.5 * unitMs

                if (duration >= wordThreshold) {
                    appendSpace(" / ")
                    // Calibrate unitMs based on word gap (7 units)
                    val estimatedUnit = duration / 7.0
                    if (estimatedUnit < 400.0) {
                        unitMs = (unitMs * 0.9) + (estimatedUnit * 0.1)
                    }
                } else if (duration >= charThreshold) {
                    appendSpace(" ")
                    // Calibrate unitMs based on letter gap (3 units)
                    val estimatedUnit = duration / 3.0
                    unitMs = (unitMs * 0.9) + (estimatedUnit * 0.1)
                } else {
                    // Part space (1 unit) - adjust unitMs slightly
                    unitMs = (unitMs * 0.95) + (duration * 0.05)
                }
            }

            // Clamp unitMs to reasonable boundaries (80ms to 450ms) to prevent tracking drift
            unitMs = unitMs.coerceIn(80.0, 450.0)

            lastState = isLightOn
            lastStateChangeTime = now
            triggerCallback()
        }
    }

    private fun appendSignal(sig: String) {
        signalBuffer.append(sig)
    }

    private fun appendSpace(sp: String) {
        val current = signalBuffer.toString()
        if (current.isEmpty()) return

        if (sp == " / ") {
            if (current.endsWith(" ")) {
                // Upgrade letter space to word space
                val trimmed = current.trimEnd()
                signalBuffer.setLength(0)
                signalBuffer.append(trimmed)
            }
            if (!signalBuffer.endsWith(" / ")) {
                signalBuffer.append(" / ")
            }
        } else if (sp == " ") {
            if (!current.endsWith(" ") && !current.endsWith(" / ")) {
                signalBuffer.append(" ")
            }
        }
    }

    private fun triggerCallback() {
        val rawSignals = signalBuffer.toString()
        val decodedText = translateMorseSequence(rawSignals)
        onTextDecoded(decodedText, rawSignals)
    }

    /**
     * Clear all buffers to start a new decoding session.
     */
    fun clear() {
        signalBuffer.setLength(0)
        lastState = false
        lastStateChangeTime = System.currentTimeMillis()
        unitMs = 200.0 // Reset calibration
        triggerCallback()
    }

    private fun translateMorseSequence(sequence: String): String {
        if (sequence.isEmpty()) return ""
        // Split by word gaps
        val words = sequence.split(" / ")
        val decodedWords = words.map { word ->
            val letters = word.trim().split(" ")
            val decodedLetters = letters.map { letter ->
                REVERSE_MORSE_CODE_MAP[letter] ?: ""
            }
            decodedLetters.joinToString("")
        }
        return decodedWords.filter { it.isNotEmpty() }.joinToString(" ")
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
