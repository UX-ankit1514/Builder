package com.uxankit.stepwise

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.uxankit.stepwise.platform.androidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The cream canvas is always light, so keep dark status and navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val platform = androidPlatform(this, webClientId())
        setContent { App(platform) }
    }

    /**
     * The google-services plugin turns the OAuth web client in google-services.json into
     * R.string.default_web_client_id. It is looked up by name so the app still builds (and
     * explains what is missing) if Google sign-in was not enabled when the file was downloaded.
     */
    @SuppressLint("DiscouragedApi")
    private fun webClientId(): String? {
        val id = resources.getIdentifier("default_web_client_id", "string", packageName)
        return if (id != 0) getString(id).takeIf { it.isNotBlank() } else null
    }
}
