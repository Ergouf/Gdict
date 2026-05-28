package io.github.gdict

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.gdict.ui.GdictApp
import io.github.gdict.util.LocaleHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GdictApp()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("gdict_data", Context.MODE_PRIVATE)
            .getString("language", "") ?: ""
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, lang))
    }
}
