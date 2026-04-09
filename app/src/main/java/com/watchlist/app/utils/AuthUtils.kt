package com.watchlist.app.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import java.security.SecureRandom

object AuthUtils {
    private const val ALLOWED_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~"
    private const val PREFS_NAME = "mal_auth_prefs"
    private const val KEY_VERIFIER = "code_verifier"
    private const val KEY_TOKEN = "access_token" // Acá guardaremos el Pase VIP

    // El tubo por donde viajará el código desde la Activity al ViewModel
    val authCodeFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    fun generateAndSaveVerifier(context: Context): String {
        val secureRandom = SecureRandom()
        val sb = java.lang.StringBuilder(128)
        for (i in 0 until 128) {
            sb.append(ALLOWED_CHARS[secureRandom.nextInt(ALLOWED_CHARS.length)])
        }
        val verifier = sb.toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_VERIFIER, verifier).apply()
        return verifier
    }

    fun getSavedVerifier(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_VERIFIER, null)
    }

    // Funciones para manejar el Pase VIP definitivo
    fun saveAccessToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAccessToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }
}