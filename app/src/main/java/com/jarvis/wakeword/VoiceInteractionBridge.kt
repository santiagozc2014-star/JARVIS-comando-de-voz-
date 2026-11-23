package com.jarvis.wakeword

import android.content.Context
import android.content.Intent
import android.service.voice.VoiceInteractionService
import android.util.Log

/** Use the Android assistant route only when JARVIS is the selected assistant. */
object VoiceInteractionBridge {
    private const val TAG = "VoiceInteractionBridge"

    fun requestSession(context: Context): Boolean {
        val active = try {
            VoiceInteractionService.isActiveService(
                context,
                JarvisVoiceInteractionService.component(context)
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo comprobar el asistente activo", e)
            false
        }
        if (!active) {
            Log.i(TAG, "JARVIS no es el asistente predeterminado; se usará la ruta anterior")
            return false
        }
        return try {
            context.sendBroadcast(
                Intent(JarvisVoiceInteractionService.ACTION_WAKE_WORD_DETECTED)
                    .setPackage(context.packageName)
            )
            Log.i(TAG, "Detección enviada a la sesión de asistente del sistema")
            true
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo enviar la activación al asistente del sistema", e)
            false
        }
    }
}
