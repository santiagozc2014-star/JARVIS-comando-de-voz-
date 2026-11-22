package com.jarvis.wakeword

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.role.RoleManager
import android.os.Build
import android.net.Uri
import android.util.Log

object ChatGPTLauncher {

    private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    private const val TAG = "ChatGPTLauncher"

    /**
     * Prefer Android's public assistant entry point when ChatGPT is the
     * configured system assistant. On Android, this is the supported route
     * used by the system assistant gesture; it may open ChatGPT Voice.
     * If Android blocks a background launch, the caller can offer a
     * notification-based, user-tapped fallback.
     */
    fun launchVoiceAssistant(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isChatGptDefaultAssistant(context)) {
            try {
                val assistIntent = Intent(Intent.ACTION_ASSIST).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(assistIntent)
                Log.i(TAG, "Android default assistant invoked for ChatGPT")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Android blocked the assistant launch; trying app launcher", e)
            }
        }
        return launch(context)
    }

    private fun isChatGptDefaultAssistant(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.getRoleHolders(RoleManager.ROLE_ASSISTANT)
                ?.contains(CHATGPT_PACKAGE) == true
        } catch (e: Exception) {
            Log.w(TAG, "Could not verify Android assistant role", e)
            false
        }
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
