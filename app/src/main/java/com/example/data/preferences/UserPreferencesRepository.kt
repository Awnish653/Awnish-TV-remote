package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val hapticFeedbackEnabled: Boolean = true,
    val themeMode: String = "DARK", // DARK, LIGHT, SYSTEM
    val buttonLayout: String = "STANDARD", // STANDARD, COMPACT
    val autoReconnectEnabled: Boolean = true,
    val testDemoModeEnabled: Boolean = false
)

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("awnish_remote_user_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            hapticFeedbackEnabled = prefs.getBoolean("haptic_feedback", true),
            themeMode = prefs.getString("theme_mode", "DARK") ?: "DARK",
            buttonLayout = prefs.getString("button_layout", "STANDARD") ?: "STANDARD",
            autoReconnectEnabled = prefs.getBoolean("auto_reconnect", true),
            testDemoModeEnabled = prefs.getBoolean("test_demo_mode", false)
        )
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
        _preferences.value = _preferences.value.copy(hapticFeedbackEnabled = enabled)
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _preferences.value = _preferences.value.copy(themeMode = mode)
    }

    fun setButtonLayout(layout: String) {
        prefs.edit().putString("button_layout", layout).apply()
        _preferences.value = _preferences.value.copy(buttonLayout = layout)
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_reconnect", enabled).apply()
        _preferences.value = _preferences.value.copy(autoReconnectEnabled = enabled)
    }

    fun setTestDemoModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("test_demo_mode", enabled).apply()
        _preferences.value = _preferences.value.copy(testDemoModeEnabled = enabled)
    }
}
