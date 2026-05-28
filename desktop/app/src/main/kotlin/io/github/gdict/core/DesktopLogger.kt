package io.github.gdict.core

class DesktopLogger : GdictLogger {
    override fun d(tag: String, msg: String) {
        println("[DEBUG] [$tag] $msg")
    }

    override fun i(tag: String, msg: String) {
        println("[INFO] [$tag] $msg")
    }

    override fun w(tag: String, msg: String) {
        System.err.println("[WARN] [$tag] $msg")
    }

    override fun e(tag: String, msg: String, throwable: Throwable?) {
        System.err.println("[ERROR] [$tag] $msg")
        throwable?.printStackTrace(System.err)
    }
}
