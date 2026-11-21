package com.jarvis.wakeword

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

object ChatGPTLauncher {

    private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    private const val TAG = "ChatGPTLauncher"

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
                Log.i(TAG, "ChatGPT abierto correctamente")
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
