package com.jarvis.wakeword

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

object ChatGPTLauncher {

    private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    private const val TAG = "ChatGPTLauncher"

    /**
     * Ask Android to invoke the configured system assistant. The user has
     * already selected ChatGPT as the device assistant, so there is no need
     * to query protected role-holder information from the app.
     *
     * If the system cannot resolve or launch the assistant, fall back to
     * opening the ChatGPT application. A foreground-service launch can still
     * be restricted by Android; the notification remains the user-tapped
     * fallback in that case.
     */
    fun launchVoiceAssistant(context: Context): Boolean {
        try {
            val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(assistIntent)
            Log.i(TAG, "Intent ACTION_ASSIST enviado al sistema")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo invocar el asistente del sistema; intentando abrir ChatGPT", e)
        }
        return launch(context)
    }

    fun launch(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(CHATGPT_PACKAGE)
        if (launchIntent != null) {
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            try {
                context.startActivity(launchIntent)
                Log.i(TAG, "Intent de apertura de ChatGPT enviado al sistema")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo abrir ChatGPT directamente", e)
            }
        }

        try {
            val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$CHATGPT_PACKAGE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            Log.i(TAG, "Abriendo Play Store (ChatGPT no instalado)")
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo abrir ChatGPT ni Play Store", e)
        }
        return false
    }

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(CHATGPT_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
