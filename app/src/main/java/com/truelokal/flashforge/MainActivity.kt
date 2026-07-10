package com.truelokal.flashforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.truelokal.flashforge.viewmodel.FlashlightViewModel

/**
 * Single-activity entry point for FlashForge.
 *
 * Responsibilities:
 * - Edge-to-edge display setup
 * - Theme preference state (follows system by default)
 * - Lifecycle observation (auto-off on pause)
 */
class MainActivity : ComponentActivity() {

    private lateinit var flashlightViewModel: FlashlightViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        flashlightViewModel = ViewModelProvider(this)[FlashlightViewModel::class.java]

        // Auto-off flashlight when the app goes to background
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                flashlightViewModel.onPause()
            }
        })

        setContent {
            val systemDark = isSystemInDarkTheme()
            // null = follow system, true = forced dark, false = forced light
            var themeOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val isDark = themeOverride ?: systemDark

            FlashForgeApp(
                isDarkTheme = isDark,
                onToggleTheme = {
                    themeOverride = !isDark
                },
            )
        }
    }
}
