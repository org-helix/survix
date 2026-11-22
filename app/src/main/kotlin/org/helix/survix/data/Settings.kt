package org.helix.survix.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Language(val tag: String) { SYSTEM(""), FRENCH("fr"), ENGLISH("en") }

/**
 * User preferences. The theme goes through AppCompat's night mode, so the window,
 * the system bars and Compose all agree; the language goes through AppCompat's per-app
 * locales, which Android 13+ also shows in the system settings.
 */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM),
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    /** False until the welcome screen has been completed once. */
    var welcomeDone: Boolean
        get() = prefs.getBoolean(KEY_WELCOME, false)
        set(value) = prefs.edit { putBoolean(KEY_WELCOME, value) }

    fun setTheme(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _theme.value = mode
        applyTheme()
    }

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            when (_theme.value) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            },
        )
    }

    val language: Language
        get() {
            val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return Language.entries.firstOrNull { it.tag.isNotEmpty() && tag.startsWith(it.tag) } ?: Language.SYSTEM
        }

    fun setLanguage(language: Language) {
        AppCompatDelegate.setApplicationLocales(
            if (language == Language.SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.tag),
        )
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_WELCOME = "welcome_done"
    }
}
