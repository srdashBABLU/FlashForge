package com.truelokal.flashforge.service

/**
 * Decodes light state intervals into Morse signals (dots, dashes, spaces)
 * and translates them to English text in real-time.
 *
 * It acts on dynamic state transitions:
 * - When light stays ON: records length to identify Dot vs Dash.
 * - When light stays OFF: records length to identify intra-character gap,
 *   character gap, or word gap (space).
 */
class MorseSignalDecoder(
    private val onTextDecoded: (String, String) -> Unit // returns (decodedText, rawSignals)
) {
    private var lastState = false
    private var lastStateChangeTime = System.currentTimeMillis()
    private val signalBuffer = StringBuilder()
    private val textBuffer = StringBuilder()

    // Typical Morse parameters (flexible thresholds based on unit duration of ~120-250ms)
    // We calibrate dynamically, assuming a base unit duration of ~150ms
    private val unitMs = 150L

    /**
     * Feed the live detected state of light.
     */
    fun onLightStateChanged(isLightOn: Boolean) {
        val now = System.currentTimeMillis()
        val duration = now - lastStateChangeTime

        if (isLightOn != lastState) {
            // Process the previous state's duration
            if (lastState) {
                // Light was ON: classify as Dot vs Dash
                if (duration in 40..350) {
                    signalBuffer.append("·")
                } else if (duration in 350..1200) {
                    signalBuffer.append("—")
                }
            } else {
                // Light was OFF: check for character/word gaps
                if (duration in 400..1100) {
                    // Character gap: parse current signal buffer
                    decodeCurrentCharacter()
                } else if (duration in 1100..4000) {
                    // Word gap: parse current character and add space
                    decodeCurrentCharacter()
                    if (textBuffer.isNotEmpty() && !textBuffer.endsWith(" ")) {
                        textBuffer.append(" ")
                    }
                }
            }

            lastState = isLightOn
            lastStateChangeTime = now
            triggerCallback()
        } else {
            // If the light has been OFF for a very long time, automatically flush the remaining buffer
            if (!isLightOn && duration > 2000 && signalBuffer.isNotEmpty()) {
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
