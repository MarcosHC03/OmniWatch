package com.watchlist.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.watchlist.app.navigation.WatchListNavHost
import com.watchlist.app.ui.theme.WatchListTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WatchListTheme {
                WatchListNavHost()
            }
        }
    }

    // Esta función ataja el link de Chrome cuando la app ya estaba abierta (singleTask)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Reemplazamos el "buzón viejo" por este nuevo que trae el código
        setIntent(intent)
    }

    // Radar MAL
    override fun onResume() {
        super.onResume()
        
        val uri = intent?.data
        if (uri != null && uri.scheme == "omniwatch" && uri.host == "callback") {
            val authCode = uri.getQueryParameter("code")
            
            if (authCode != null) {
                // Tiramos el código por el tubo secreto
                com.watchlist.app.utils.AuthUtils.authCodeFlow.tryEmit(authCode)
                intent = null 
            } else {
                val error = uri.getQueryParameter("error")
                android.widget.Toast.makeText(this, "MAL canceló: $error", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
