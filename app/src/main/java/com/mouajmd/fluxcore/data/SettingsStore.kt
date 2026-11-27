package com.mouajmd.fluxcore.data

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LanguageMode(val tag: String) {
    SYSTEM(""),
    ZH("zh-CN"),
    EN("en")
}

@Immutable
data class AppSettings(
    val advancedMode: Boolean = false,
    val hapticEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val paletteIndex: Int = 0,
    val languageMode: LanguageMode = LanguageMode.SYSTEM,
    val hidePersistentNotification: Boolean = false
)

object SettingsStore {
    private const val PREF_SETTINGS = "fluxcore_settings_v2"
    private const val PREF_LEGACY = "fluxcore_rules_v1"
    private const val KEY_ADVANCED = "advanced_mode"
    private const val KEY_HAPTIC = "haptic_enabled"
    private const val KEY_HIDE_PERSISTENT_NOTIF = "hide_persistent_notification"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_PALETTE_INDEX = "palette_index"
    private const val KEY_LANGUAGE = "language_mode"

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val isHapticEnabled: Boolean
        get() = _settings.value.hapticEnabled

    fun init(context: Context) {
        val appCtx = context.applicationContext
        val sp = appCtx.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        val legacySp = appCtx.getSharedPreferences(PREF_LEGACY, Context.MODE_PRIVATE)

        val advanced = legacySp.getBoolean(KEY_ADVANCED, sp.getBoolean(KEY_ADVANCED, false))
        val haptic = sp.getBoolean(KEY_HAPTIC, true)
        val hideNotif = sp.getBoolean(KEY_HIDE_PERSISTENT_NOTIF, false)
        val theme = runCatching {
            ThemeMode.valueOf(sp.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        val dynamic = sp.getBoolean(KEY_DYNAMIC_COLOR, true)
        val palette = sp.getInt(KEY_PALETTE_INDEX, 0).coerceIn(0, 6)
        val lang = readCurrentLanguageMode(appCtx, sp)

        _settings.value = AppSettings(
            advancedMode = advanced,
            hapticEnabled = haptic,
            themeMode = theme,
            dynamicColor = dynamic,
            paletteIndex = palette,
            languageMode = lang,
            hidePersistentNotification = hideNotif
        )
    }

    private fun readCurrentLanguageMode(context: Context, sp: SharedPreferences): LanguageMode {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java)
            val locales = lm?.applicationLocales
            if (locales != null && !locales.isEmpty) {
                val tag = locales[0].language
                return when {
                    tag.startsWith("en", ignoreCase = true) -> LanguageMode.EN
                    tag.startsWith("zh", ignoreCase = true) -> LanguageMode.ZH
                    else -> LanguageMode.SYSTEM
                }
            }
            return LanguageMode.SYSTEM
        }
        val saved = sp.getString(KEY_LANGUAGE, LanguageMode.SYSTEM.name) ?: LanguageMode.SYSTEM.name
        return runCatching { LanguageMode.valueOf(saved) }.getOrDefault(LanguageMode.SYSTEM)
    }

    fun setAdvancedMode(context: Context, enabled: Boolean) {
        val appCtx = context.applicationContext
        appCtx.getSharedPreferences(PREF_LEGACY, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ADVANCED, enabled).apply()
        appCtx.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ADVANCED, enabled).apply()
        _settings.value = _settings.value.copy(advancedMode = enabled)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _settings.value = _settings.value.copy(hapticEnabled = enabled)
    }

    fun isHidePersistentNotification(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            .getBoolean(KEY_HIDE_PERSISTENT_NOTIF, false)

    fun setHidePersistentNotification(context: Context, hide: Boolean) {
        context.applicationContext.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HIDE_PERSISTENT_NOTIF, hide).apply()
        _settings.value = _settings.value.copy(hidePersistentNotification = hide)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.applicationContext.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setDynamicColor(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _settings.value = _settings.value.copy(dynamicColor = enabled)
    }

    fun setPaletteIndex(context: Context, index: Int) {
        val safe = index.coerceIn(0, 6)
        context.applicationContext.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PALETTE_INDEX, safe)
            .putBoolean(KEY_DYNAMIC_COLOR, false)
            .apply()
        _settings.value = _settings.value.copy(paletteIndex = safe, dynamicColor = false)
    }

    fun setLanguageMode(context: Context, mode: LanguageMode) {
        context.applicationContext.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, mode.name).apply()
        _settings.value = _settings.value.copy(languageMode = mode)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java)
            lm?.applicationLocales = when (mode) {
                LanguageMode.SYSTEM -> LocaleList.getEmptyLocaleList()
                LanguageMode.ZH -> LocaleList.forLanguageTags("zh-CN")
                LanguageMode.EN -> LocaleList.forLanguageTags("en")
            }
        }
    }

    fun wrapContextForLegacyLocale(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val sp = base.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        val mode = runCatching {
            LanguageMode.valueOf(sp.getString(KEY_LANGUAGE, LanguageMode.SYSTEM.name) ?: LanguageMode.SYSTEM.name)
        }.getOrDefault(LanguageMode.SYSTEM)
        val locale = when (mode) {
            LanguageMode.ZH -> Locale.SIMPLIFIED_CHINESE
            LanguageMode.EN -> Locale.ENGLISH
            LanguageMode.SYSTEM -> return base
        }
        val config = base.resources.configuration
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
