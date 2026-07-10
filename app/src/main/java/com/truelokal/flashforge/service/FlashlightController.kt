package com.truelokal.flashforge.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hardware abstraction layer for flashlight control using Camera2 API.
 *
 * Supports:
 * - Basic on/off toggle (all devices with flash, API 23+)
 * - Variable intensity control (API 33+, devices with FLASH_INFO_STRENGTH_MAXIMUM_LEVEL > 1)
 *
 * No CAMERA permission is required for torch control.
 */
class FlashlightController(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _currentStrength = MutableStateFlow(0)
    val currentStrength: StateFlow<Int> = _currentStrength.asStateFlow()

    /** The camera ID that has the flash unit. Null if none found. */
    val flashCameraId: String? = findFlashCameraId()

    /** Whether the device has a flash unit at all. */
    val hasFlash: Boolean = flashCameraId != null

    /** Maximum torch brightness level. 1 means on/off only; >1 means variable. */
    val maxStrengthLevel: Int = getMaxStrength()

    /** Whether variable intensity is supported (API 33+ and hardware support). */
    val supportsIntensity: Boolean = maxStrengthLevel > 1

    /** Default torch brightness level. */
    val defaultStrengthLevel: Int = getDefaultStrength()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashCameraId) {
                _isTorchOn.value = enabled
                if (!enabled) {
                    _currentStrength.value = 0
                }
            }
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            if (cameraId == flashCameraId) {
                _currentStrength.value = newStrengthLevel
            }
        }
    }

    init {
        // Register torch callback to monitor state changes from external sources
        // (e.g., quick settings tile, other apps)
        cameraManager.registerTorchCallback(torchCallback, null)
    }

    /**
     * Toggle the flashlight on or off.
     */
    fun toggleTorch(): Boolean {
        val cameraId = flashCameraId ?: return false
        return try {
            val newState = !_isTorchOn.value
            cameraManager.setTorchMode(cameraId, newState)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Turn the flashlight on.
     */
    fun turnOn(): Boolean {
        val cameraId = flashCameraId ?: return false
        return try {
            cameraManager.setTorchMode(cameraId, true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Turn the flashlight off.
     */
    fun turnOff(): Boolean {
        val cameraId = flashCameraId ?: return false
        return try {
            cameraManager.setTorchMode(cameraId, false)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Set the torch brightness to a specific level.
     *
     * @param level Brightness level, 1 to [maxStrengthLevel].
     *              If 0, turns off the torch.
     * @return true if the operation succeeded.
     */
    fun setStrength(level: Int): Boolean {
        val cameraId = flashCameraId ?: return false

        if (level <= 0) {
            return turnOff()
        }

        if (!supportsIntensity) {
            // Device doesn't support variable intensity, just turn on
            return turnOn()
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val clampedLevel = level.coerceIn(1, maxStrengthLevel)
                cameraManager.turnOnTorchWithStrengthLevel(cameraId, clampedLevel)
            } else {
                // Fallback to basic on/off
                cameraManager.setTorchMode(cameraId, true)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Release resources. Call when the controller is no longer needed.
     */
    fun release() {
        try {
            // Turn off the torch when releasing
            flashCameraId?.let {
                if (_isTorchOn.value) {
                    cameraManager.setTorchMode(it, false)
                }
            }
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findFlashCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getMaxStrength(): Int {
        val cameraId = flashCameraId ?: return 1
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val maxLevel = characteristics.get(
                    CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL
                )
                maxLevel ?: 1
            } else {
                1 // Pre-API 33: on/off only
            }
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }

    private fun getDefaultStrength(): Int {
        val cameraId = flashCameraId ?: return 1
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val defaultLevel = characteristics.get(
                    CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL
                )
                defaultLevel ?: 1
            } else {
                1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            1
        }
    }
}
