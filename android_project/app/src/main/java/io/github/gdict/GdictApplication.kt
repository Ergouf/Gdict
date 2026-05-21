package io.github.gdict

import android.app.Application
import io.github.gdict.data.AppRepository

class GdictApplication : Application() {
    val repository: AppRepository by lazy { AppRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GdictApplication
            private set
    }
}
