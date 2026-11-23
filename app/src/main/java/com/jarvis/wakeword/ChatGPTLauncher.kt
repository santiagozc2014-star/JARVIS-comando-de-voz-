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
     * Tries ChatGPT first, then the configured system assistant, then Play Store.
     *
     * The result reports only that Android accepted startActivity() without
     * throwing. It does NOT prove that Android displayed the target app:
     * background activity launch restrictions can block that silently.
     */
    fun requestVoiceAssistantLaunch(context: Context): LaunchRequestResult {
        return LaunchRequestPolicy.execute(object : LaunchRequestGateway {
            override fun requestAssistant(): Boolean {
                return try {
                    context.startActivity(Intent(Intent.ACTION_ASSIST).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    Log.i(TAG, "Android aceptó la solicitud ACTION_ASSIST; apertura no confirmada")
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo solicitar ACTION_ASSIST", e)
                    false
                }
            }

            override fun requestChatGPT(): Boolean {
                return try {
                    val launchIntent = context.packageManager
                        .getLaunchIntentForPackage(CHATGPT_PACKAGE) ?: return false
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                    context.startActivity(launchIntent)
                    Log.i(TAG, "Android aceptó la solicitud de apertura de ChatGPT; apertura no confirmada")
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo solicitar la apertura de ChatGPT", e)
                    false
                }
            }

            override fun requestPlayStore(): Boolean {
                return try {
                    val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://details?id=$CHATGPT_PACKAGE")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(marketIntent)
                    Log.i(TAG, "Android aceptó la solicitud de apertura de Play Store")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo solicitar la apertura de Play Store", e)
                    false
                }
            }
        })
    }

    /**
     * User-initiated app launch used by the manual test button.
     * A true result still means only that startActivity() did not throw.
     */
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
                Log.i(TAG, "Solicitud manual de apertura de ChatGPT enviada")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo abrir ChatGPT directamente", e)
            }
        }

        try {
            val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$CHATGPT_PACKAGE"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            Log.i(TAG, "Solicitud de apertura de Play Store enviada (ChatGPT no instalado)")
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
