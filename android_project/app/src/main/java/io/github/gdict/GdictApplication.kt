package io.github.gdict

import android.app.Application
import android.util.Log
import io.github.gdict.core.GdictLogger
import io.github.gdict.data.AppRepository

class GdictApplication : Application() {
    val repository: AppRepository by lazy { AppRepository(this) }

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
