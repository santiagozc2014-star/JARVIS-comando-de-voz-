package com.jarvis.wakeword

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Lightweight system-owned assistant entry point. Android keeps the selected
 * service running for hotword interactions; session work runs in another process.
 */
class JarvisVoiceInteractionService : VoiceInteractionService() {
    private var isReady = false
    private var pendingWakeRequest = false
    private var receiverRegistered = false

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_WAKE_WORD_DETECTED) return
            if (!isReady) {
                pendingWakeRequest = true
                Log.i(TAG, "Activación recibida antes de onReady; queda pendiente")
                return
            }
            showJarvisSession()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            wakeReceiver,
            IntentFilter(ACTION_WAKE_WORD_DETECTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
        Log.i(TAG, "VoiceInteractionService creado")
    }

    override fun onReady() {
        super.onReady()
        isReady = true
        Log.i(TAG, "VoiceInteractionService listo")
        if (pendingWakeRequest) {
            pendingWakeRequest = false
            showJarvisSession()
        }
    }

    override fun onShutdown() {
        isReady = false
        pendingWakeRequest = false
        super.onShutdown()
    }

    private fun showJarvisSession() {
        if (!isReady) return
        try {
            // No assist data or screenshots are requested from the foreground app.
            showSession(Bundle(), 0)
            Log.i(TAG, "Solicitud de sesión de voz enviada al sistema")
        } catch (e: Exception) {
            Log.e(TAG, "Android no pudo mostrar la sesión de JARVIS", e)
        }
    }

    override fun onDestroy() {
        isReady = false
        pendingWakeRequest = false
        if (receiverRegistered) {
            try {
                unregisterReceiver(wakeReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo retirar el receptor de activación", e)
            }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_WAKE_WORD_DETECTED =
            "com.jarvis.wakeword.action.WAKE_WORD_DETECTED"
        private const val TAG = "JarvisVoiceInteraction"

        fun component(context: Context) =
            android.content.ComponentName(context, JarvisVoiceInteractionService::class.java)
    }
}
