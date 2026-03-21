package com.alpha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alpha.features.settings.AppSettings
import com.alpha.ui.navigation.AlphaNavGraph
import com.alpha.ui.theme.AlphaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            val scope = rememberCoroutineScope()

            // Read persisted theme settings from DataStore
            val useSystem by AppSettings.useSystemThemeFlow(applicationContext)
                .collectAsStateWithLifecycle(initialValue = true)
            val savedDark by AppSettings.darkModeFlow(applicationContext)
                .collectAsStateWithLifecycle(initialValue = systemDark)

            // Effective dark mode: follow system if enabled, else use saved preference
            val isDarkMode = if (useSystem) systemDark else savedDark

            AlphaTheme(darkTheme = isDarkMode) {
                Surface {
                    AlphaNavGraph(
                        isDarkMode = isDarkMode,
                        onThemeToggle = {
                            // Quick toggle: turn off system-follow and flip dark
                            scope.launch {
                                AppSettings.setUseSystemTheme(applicationContext, false)
                                AppSettings.setDarkMode(applicationContext, !isDarkMode)
                            }
                        }
                    )
                }
            }
        }
    }
}
