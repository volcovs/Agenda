package com.personalagenda.app.data

import android.content.Context
import com.personalagenda.app.ui.theme.AppThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists lightweight app settings (currently the theme choice) in SharedPreferences. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("agenda_settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppThemeOption> = _theme.asStateFlow()

    private fun loadTheme(): AppThemeOption =
        runCatching { AppThemeOption.valueOf(prefs.getString(KEY_THEME, null) ?: AppThemeOption.SYSTEM.name) }
            .getOrDefault(AppThemeOption.SYSTEM)

    fun setTheme(option: AppThemeOption) {
        prefs.edit().putString(KEY_THEME, option.name).apply()
        _theme.value = option
    }

    private companion object {
        const val KEY_THEME = "theme"
    }
}
