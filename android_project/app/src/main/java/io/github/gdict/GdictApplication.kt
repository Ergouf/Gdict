package io.github.gdict

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.gdict.core.GdictLogger
import io.github.gdict.data.BookmarkRepository
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.data.HistoryRepository
import io.github.gdict.data.SettingsRepository
import io.github.gdict.util.LocaleHelper

class GdictApplication : Application() {
    val dictionaryRepository: DictionaryRepository by lazy { DictionaryRepository(this) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(this) }
    val bookmarkRepository: BookmarkRepository by lazy { BookmarkRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

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
