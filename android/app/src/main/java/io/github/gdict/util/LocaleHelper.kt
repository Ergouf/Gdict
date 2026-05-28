package io.github.gdict.util

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import java.util.Locale

object LocaleHelper {

    const val LANG_FOLLOW_SYSTEM = ""
    const val LANG_ENGLISH = "en"
    const val LANG_SIMPLIFIED_CHINESE = "zh-CN"
    const val LANG_TRADITIONAL_CHINESE = "zh-TW"

    fun getLocaleFromTag(tag: String, context: Context): Locale = when (tag) {
        LANG_ENGLISH -> Locale.ENGLISH
        LANG_SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
        LANG_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE
        else -> getSystemLocale(context)
    }

    fun getSystemLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    fun wrapContext(context: Context, languageTag: String): Context {
        val locale = getLocaleFromTag(languageTag, context)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    fun applyLocaleToContextWrapper(context: Context, languageTag: String): ContextWrapper {
        return object : ContextWrapper(wrapContext(context, languageTag)) {}
    }
}
