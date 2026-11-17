package dev.master.lumio

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.master.lumio.ui.settings.SettingsViewModel
import dev.master.lumio.data.settings.Appearance
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.SideEffect
import dev.master.lumio.navigation.LumioApp
import dev.master.lumio.ui.theme.LumioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = (application as LumioApplication).container
            val settings: SettingsViewModel = viewModel { SettingsViewModel(container.settingsRepository) }
            val state by settings.state.collectAsStateWithLifecycle()
            val dark = when (state.settings.appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            LumioTheme(dark) {
                // Read saved appearance before displaying app content to avoid a wrong-theme flash.
                if (!state.initialized) Surface(Modifier.fillMaxSize()) {}
                else LumioApp(container, settings)
            }
        }
    }
}
