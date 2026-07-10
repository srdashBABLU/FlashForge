package com.truelokal.flashforge.service

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Image analyzer to process camera frames in real-time and calculate luma (luminance).
 *
 * It focuses on the central region of the camera viewfinder (30% center box)
 * to avoid noise from surrounding ambient light. It updates the min and max luma
 * dynamically to determine the threshold for detecting flash signals (ON/OFF).
 */
class MorseDecoderAnalyzer(
    private val onLuminanceResult: (Double, Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCount = 0
    private var minLuma = 255.0
    private var maxLuma = 0.0
    private var lastDynamicResetTime = System.currentTimeMillis()

    // Helper extension to copy ByteBuffer to ByteArray safely
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    override fun analyze(image: ImageProxy) {
        // The Y plane (index 0) represents the luminance channel (grayscale)
        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // Crop window: central 30% area
        val cropW = (width * 0.3).toInt()
        val cropH = (height * 0.3).toInt()
        val startX = (width - cropW) / 2
        val startY = (height - cropH) / 2

        var totalLuma = 0L
        var sampleCount = 0

        // Read Y-plane pixels in the cropped region only
        val rowBuffer = ByteArray(width) // Temp row buffer for fast retrieval
        for (y in startY until (startY + cropH)) {
            buffer.position(y * rowStride)
            // Retrieve row segment or elements depending on pixelStride
            if (pixelStride == 1) {
                buffer.get(rowBuffer, 0, width)
                for (x in startX until (startX + cropW)) {
                    val lumaVal = rowBuffer[x].toInt() and 0xFF
                    totalLuma += lumaVal
                    sampleCount++
                }
            } else {
                // Slower fallback for pixelStride > 1
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

        // Dynamically adjust min/max threshold every 5 seconds to adapt to changes in ambient room lighting
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDynamicResetTime > 5000) {
            // Decaying dynamic adaptation
            minLuma = (minLuma * 0.6) + (avgLuma * 0.4)
            maxLuma = (maxLuma * 0.6) + (avgLuma * 0.4)
            lastDynamicResetTime = currentTime
        } else {
            if (avgLuma < minLuma) minLuma = avgLuma
            if (avgLuma > maxLuma) maxLuma = avgLuma
        }

        // Avoid division by zero and minimal variation issues
        val lumaRange = maxLuma - minLuma
        val isLightOn = if (lumaRange > 20.0) {
            // Brightness threshold: 60% mark between low and high
            val threshold = minLuma + (lumaRange * 0.6)
            avgLuma > threshold
        } else {
            false // Light difference is too low to distinguish
        }

        // Notify client viewmodel
        onLuminanceResult(avgLuma, isLightOn)

        // Close image to release resources and trigger the next analyzer frame
        image.close()
    }
}
