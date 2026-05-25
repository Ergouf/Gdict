package io.github.gdict

import android.app.Application
import android.util.Log
import io.github.gdict.core.GdictLogger
import io.github.gdict.data.BookmarkRepository
import io.github.gdict.data.DictionaryRepository
import io.github.gdict.data.HistoryRepository
import io.github.gdict.data.SettingsRepository

class GdictApplication : Application() {
    val dictionaryRepository: DictionaryRepository by lazy { DictionaryRepository(this) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(this) }
    val bookmarkRepository: BookmarkRepository by lazy { BookmarkRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        GdictLogger.setLogger(AndroidLogger)
    }
}

private object AndroidLogger : GdictLogger {
    override fun d(tag: String, msg: String) { Log.d(tag, msg) }
    override fun i(tag: String, msg: String) { Log.i(tag, msg) }
    override fun w(tag: String, msg: String) { Log.w(tag, msg) }
    override fun e(tag: String, msg: String, throwable: Throwable?) { Log.e(tag, msg, throwable) }
}
