package com.truelokal.flashforge.service

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Image analyzer to process camera frames in real-time and calculate luma (luminance).
 * Uses a sliding history window for smooth threshold adjustments and a hysteresis filter
 * to handle frame rate jitter and noise on low-end devices.
 */
class MorseDecoderAnalyzer(
    private val onLuminanceResult: (Double, Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val lumaHistory = ArrayList<Double>()
    private val maxHistorySize = 45 // Moving window (approx 1.5 seconds of frames)
    private var lastPublishedState = false

    override fun analyze(image: ImageProxy) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // Crop window: central 30% area to isolate the flash source
        val cropW = (width * 0.3).toInt()
        val cropH = (height * 0.3).toInt()
        val startX = (width - cropW) / 2
        val startY = (height - cropH) / 2

        var totalLuma = 0L
        var sampleCount = 0

        val rowBuffer = ByteArray(width)
        for (y in startY until (startY + cropH)) {
            buffer.position(y * rowStride)
            if (pixelStride == 1) {
                buffer.get(rowBuffer, 0, width)
                for (x in startX until (startX + cropW)) {
                    val lumaVal = rowBuffer[x].toInt() and 0xFF
                    totalLuma += lumaVal
                    sampleCount++
                }
            } else {
                for (x in startX until (startX + cropW)) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val lumaVal = buffer.get(index).toInt() and 0xFF
                        totalLuma += lumaVal
                        sampleCount++
                    }
                }
            }
        }

        val avgLuma = if (sampleCount > 0) totalLuma.toDouble() / sampleCount else 0.0

        // Maintain sliding history window for smooth tracking of min/max values
        lumaHistory.add(avgLuma)
        if (lumaHistory.size > maxHistorySize) {
            lumaHistory.removeAt(0)
        }

        val minLuma = lumaHistory.minOrNull() ?: avgLuma
        val maxLuma = lumaHistory.maxOrNull() ?: avgLuma
        val lumaRange = maxLuma - minLuma

        // Hysteresis boundary threshold to prevent ON/OFF state flickering/jitter
        val isLightOn = if (lumaRange > 18.0) {
            val highThreshold = minLuma + (lumaRange * 0.58)
            val lowThreshold = minLuma + (lumaRange * 0.42)
            if (lastPublishedState) {
                avgLuma > lowThreshold
            } else {
                avgLuma > highThreshold
            }
        } else {
            false
        }

        lastPublishedState = isLightOn

        // Notify controller / ViewModel
        onLuminanceResult(avgLuma, isLightOn)

        image.close()
    }
}
