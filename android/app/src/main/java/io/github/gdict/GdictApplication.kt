package io.github.gdict

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.gdict.core.GdictLogger
import io.github.gdict.data.AndroidBookmarkRepository
import io.github.gdict.data.AndroidDictionaryRepository
import io.github.gdict.data.AndroidHistoryRepository
import io.github.gdict.data.AndroidSettingsRepository
import io.github.gdict.util.LocaleHelper

class GdictApplication : Application() {
    val dictionaryRepository: AndroidDictionaryRepository by lazy { AndroidDictionaryRepository(this) }
    val historyRepository: AndroidHistoryRepository by lazy { AndroidHistoryRepository(this) }
    val bookmarkRepository: AndroidBookmarkRepository by lazy { AndroidBookmarkRepository(this) }
    val settingsRepository: AndroidSettingsRepository by lazy { AndroidSettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        GdictLogger.setLogger(AndroidLogger)
    }

    override fun attachBaseContext(base: Context) {
        val lang = base.getSharedPreferences("gdict_data", Context.MODE_PRIVATE)
            .getString("language", "") ?: ""
        super.attachBaseContext(LocaleHelper.wrapContext(base, lang))
    }
}

private object AndroidLogger : GdictLogger {
    override fun d(tag: String, msg: String) { Log.d(tag, msg) }
    override fun i(tag: String, msg: String) { Log.i(tag, msg) }
    override fun w(tag: String, msg: String) { Log.w(tag, msg) }
    override fun e(tag: String, msg: String, throwable: Throwable?) { Log.e(tag, msg, throwable) }
}
