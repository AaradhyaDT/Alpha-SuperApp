package com.alpha.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    data class UiState(
        val useSystemTheme: Boolean  = true,
        val darkMode: Boolean        = false,
        val btDeviceName: String     = AppSettings.DEFAULT_BT_NAME,
        val stabilityFrames: Int     = AppSettings.DEFAULT_STABILITY_FRAMES
    )

    val uiState: StateFlow<UiState> = combine(
        AppSettings.useSystemThemeFlow(ctx),
        AppSettings.darkModeFlow(ctx),
        AppSettings.btDeviceNameFlow(ctx),
        AppSettings.stabilityFramesFlow(ctx)
    ) { useSystem, dark, btName, frames ->
        UiState(useSystem, dark, btName, frames)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setUseSystemTheme(value: Boolean) = viewModelScope.launch {
        AppSettings.setUseSystemTheme(ctx, value)
    }

    fun setDarkMode(value: Boolean) = viewModelScope.launch {
        AppSettings.setDarkMode(ctx, value)
    }

    fun setBtDeviceName(value: String) = viewModelScope.launch {
        AppSettings.setBtDeviceName(ctx, value)
    }

    fun setStabilityFrames(value: Int) = viewModelScope.launch {
        AppSettings.setStabilityFrames(ctx, value)
    }
}
