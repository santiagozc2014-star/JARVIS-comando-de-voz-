package com.jarvis.wakeword

import android.content.Context
import android.media.AudioRecord
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())

    private val healthCheck = Runnable {
        if (!isListening) return@Runnable

        try {
            val engine = detector ?: throw IllegalStateException("El motor no existe")
            val threadField = engine.javaClass.getDeclaredField("processingThread").apply {
                isAccessible = true
            }
            val audioField = engine.javaClass.getDeclaredField("audioRecord").apply {
                isAccessible = true
            }
            val worker = threadField.get(engine) as? Thread
            val recorder = audioField.get(engine) as? AudioRecord

            val failure = when {
                worker == null -> "El motor de reconocimiento no creó su hilo de audio."
                !worker.isAlive -> "El motor de reconocimiento se detuvo durante la inicialización."
                recorder == null -> "El motor no pudo crear el capturador del micrófono."
                recorder.state != AudioRecord.STATE_INITIALIZED ->
                    "Android no pudo inicializar el capturador de audio."
                recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING ->
                    "El capturador existe, pero Android no está entregando audio."
                else -> null
            }

            if (failure != null) {
                Log.e(TAG, "Comprobación del motor: $failure")
                isListening = false
                onError(failure)
            } else {
                Log.i(TAG, "Motor de audio activo; hilo y AudioRecord verificados.")
            }
        } catch (e: Exception) {
            // A diagnostic failure must not stop a detector that may be working.
            Log.w(TAG, "No se pudo inspeccionar el estado interno del motor", e)
        }
    }

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
            Log.i(TAG, "Motor solicitado (threshold=$threshold); verificando captura real de audio.")
            // The library initializes ONNX models asynchronously and exposes no readiness callback.
            // Inspect its worker and AudioRecord after the initialization window.
            mainHandler.postDelayed(healthCheck, 20000L)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar detector", e)
            onError("Error al iniciar: ${e.message ?: e.javaClass.simpleName}")
            isListening = false
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(healthCheck)
        try {
            detector?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error al detener", e)
        }
        isListening = false
    }

    fun release() {
        mainHandler.removeCallbacks(healthCheck)
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
