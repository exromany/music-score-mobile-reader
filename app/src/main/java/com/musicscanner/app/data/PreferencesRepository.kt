package com.musicscanner.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing app preferences (theme, settings, etc.)
 */
class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _isDarkMode = MutableStateFlow(getDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    /**
     * Get current dark mode preference
     */
    private fun getDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    /**
     * Set dark mode preference
     */
    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkMode.value = enabled
    }

    /**
     * Toggle dark mode
     */
    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    companion object {
        private const val PREFS_NAME = "music_scanner_preferences"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
