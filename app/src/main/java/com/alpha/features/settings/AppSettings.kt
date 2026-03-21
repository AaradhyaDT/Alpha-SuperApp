package com.alpha.features.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single DataStore instance for the whole app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alpha_settings")

object AppSettings {

    // Keys
    val KEY_DARK_MODE       = booleanPreferencesKey("dark_mode")
    val KEY_USE_SYSTEM      = booleanPreferencesKey("use_system_theme")
    val KEY_BT_DEVICE_NAME  = stringPreferencesKey("bt_device_name")
    val KEY_STABILITY_FRAMES = intPreferencesKey("stability_frames")

    // Defaults
    const val DEFAULT_BT_NAME         = "HC-05"
    const val DEFAULT_STABILITY_FRAMES = 6

    // ── Flows ─────────────────────────────────────────────────────────────

    fun darkModeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_DARK_MODE] ?: false }

    fun useSystemThemeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_USE_SYSTEM] ?: true }

    fun btDeviceNameFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_BT_DEVICE_NAME] ?: DEFAULT_BT_NAME }

    fun stabilityFramesFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[KEY_STABILITY_FRAMES] ?: DEFAULT_STABILITY_FRAMES }

    // ── Writers ───────────────────────────────────────────────────────────

    suspend fun setDarkMode(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = value }
    }

    suspend fun setUseSystemTheme(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_USE_SYSTEM] = value }
    }

    suspend fun setBtDeviceName(context: Context, value: String) {
        context.dataStore.edit { it[KEY_BT_DEVICE_NAME] = value.trim() }
    }

    suspend fun setStabilityFrames(context: Context, value: Int) {
        context.dataStore.edit { it[KEY_STABILITY_FRAMES] = value.coerceIn(1, 30) }
    }
}
