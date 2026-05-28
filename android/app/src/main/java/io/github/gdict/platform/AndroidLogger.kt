package io.github.gdict.platform

import android.util.Log
import io.github.gdict.core.GdictLogger

class AndroidLogger : GdictLogger {
    override fun d(tag: String, msg: String) { Log.d(tag, msg) }
    override fun i(tag: String, msg: String) { Log.i(tag, msg) }
    override fun w(tag: String, msg: String) { Log.w(tag, msg) }
    override fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) Log.e(tag, msg, throwable) else Log.e(tag, msg)
    }
}
