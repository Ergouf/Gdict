package io.github.gdict.core

interface GdictLogger {
    fun d(tag: String, msg: String)
    fun i(tag: String, msg: String)
    fun w(tag: String, msg: String)
    fun e(tag: String, msg: String, throwable: Throwable? = null)

    companion object {
        private var impl: GdictLogger = NoOpLogger

        fun setLogger(logger: GdictLogger) {
            impl = logger
        }

        fun get(): GdictLogger = impl
    }

    private object NoOpLogger : GdictLogger {
        override fun d(tag: String, msg: String) {}
        override fun i(tag: String, msg: String) {}
        override fun w(tag: String, msg: String) {}
        override fun e(tag: String, msg: String, throwable: Throwable?) {}
    }
}
