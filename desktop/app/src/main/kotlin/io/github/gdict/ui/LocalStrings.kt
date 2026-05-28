package io.github.gdict.ui

import androidx.compose.runtime.compositionLocalOf
import io.github.gdict.ui.strings.EnStrings
import io.github.gdict.ui.strings.StringResources
import io.github.gdict.ui.strings.ZhCnStrings

val LocalStrings = compositionLocalOf<StringResources> { EnStrings }

sealed class AppLanguage(val code: String, val displayName: String) {
    object English : AppLanguage("en", "English")
    object SimplifiedChinese : AppLanguage("zh-CN", "简体中文")
    object TraditionalChinese : AppLanguage("zh-TW", "繁體中文")
    object FollowSystem : AppLanguage("system", "Follow System")

    companion object {
        fun fromCode(code: String): AppLanguage = when (code) {
            "en" -> English
            "zh-CN" -> SimplifiedChinese
            "zh-TW" -> TraditionalChinese
            "system" -> FollowSystem
            else -> English
        }

        val entries = listOf(English, SimplifiedChinese, TraditionalChinese, FollowSystem)
    }
}

fun getStringResourcesForLanguage(language: AppLanguage): StringResources {
    return when (language) {
        AppLanguage.SimplifiedChinese -> ZhCnStrings
        AppLanguage.TraditionalChinese -> ZhCnStrings
        AppLanguage.English, AppLanguage.FollowSystem -> EnStrings
    }
}

fun resolveEffectiveLanguage(language: AppLanguage): AppLanguage {
    if (language != AppLanguage.FollowSystem) return language
    val locale = System.getProperty("user.language") ?: "en"
    val country = System.getProperty("user.country") ?: ""
    return when {
        locale == "zh" && country == "CN" -> AppLanguage.SimplifiedChinese
        locale == "zh" -> AppLanguage.TraditionalChinese
        else -> AppLanguage.English
    }
}
