package com.awper.lightscore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.awper.lightscore.settings.SettingsStore
import com.awper.lightscore.ui.LightscoreApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val settingsStore = SettingsStore(this)
        setContent {
            val currentDensity = LocalDensity.current
            val appDensity = Density(
                density = currentDensity.density.coerceAtLeast(3f),
                fontScale = currentDensity.fontScale.coerceAtLeast(1f)
            )
            CompositionLocalProvider(LocalDensity provides appDensity) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    LightscoreApp(settingsStore)
                }
            }
        }
    }
}
