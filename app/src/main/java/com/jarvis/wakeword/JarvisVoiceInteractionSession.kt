package com.jarvis.wakeword

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log

/**
 * Requests the destination from an Android-owned assistant session rather
 * than directly launching another app from the microphone service.
 */
class JarvisVoiceInteractionSession(context: JarvisVoiceInteractionSessionService) :
    VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        setUiEnabled(false)

        val target = packageManager.getLaunchIntentForPackage(CHATGPT_PACKAGE)
            ?: Intent(getContext(), MainActivity::class.java)

        try {
            // Available since API 26; intended for assistant-originated launches.
            startAssistantActivity(target)
            Log.i(TAG, "Solicitud de apertura enviada desde la sesión de asistente")
        } catch (assistantLaunchError: Exception) {
            Log.w(TAG, "startAssistantActivity falló; probando startVoiceActivity", assistantLaunchError)
            try {
                startVoiceActivity(target)
                Log.i(TAG, "Solicitud alternativa startVoiceActivity enviada")
            } catch (voiceLaunchError: Exception) {
                Log.e(TAG, "No se pudo abrir el destino desde la sesión de asistente", voiceLaunchError)
            }
        } finally {
            finish()
        }
    }

    companion object {
        private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    }
}
