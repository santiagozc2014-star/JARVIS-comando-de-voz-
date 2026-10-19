package com.jarvis.wakeword

import android.content.Context
import android.util.Log
import com.openwakeword.OpenWakeWord

class WakeWordDetector(
    private val context: Context,
    private val threshold: Float = 0.5f,
    private val onDetected: (Float) -> Unit,
    private val onError: (String) -> Unit
) {
    private var detector: OpenWakeWord? = null
    private var isListening = false

    fun start() {
        if (isListening) return
        try {
            detector = OpenWakeWord.Builder(context)
                .setModel(OpenWakeWord.BuiltInModel.HEY_JARVIS)
                .setThreshold(threshold.coerceIn(0.25f, 0.85f))
                .setDebounceMs(2200L)
                .build()

            detector?.start { score ->
                Log.d(TAG, "Hey Jarvis detectado! score=$score")
                onDetected(score)
            }
            isListening = true
            Log.i(TAG, "WakeWordDetector iniciado (threshold=$threshold)")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar detector", e)
            onError("Error al iniciar: ${e.message}")
            isListening = false
        }
    }

    fun stop() {
        try {
            detector?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error al detener", e)
        }
        isListening = false
    }

    fun release() {
        stop()
        try {
            detector?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error al liberar recursos", e)
        }
        detector = null
    }

    fun isListening(): Boolean = isListening

    companion object {
        private const val TAG = "WakeWordDetector"
    }
}
