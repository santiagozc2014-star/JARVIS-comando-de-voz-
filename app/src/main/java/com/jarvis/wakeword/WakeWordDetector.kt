package com.jarvis.wakeword

import android.content.Context
import android.media.AudioRecord
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.openwakeword.OpenWakeWord

/**
 * Controls the wake-word engine and verifies that its worker and microphone
 * are genuinely running. The upstream library does not expose startup errors
 * or a readiness callback, so this class monitors its internal worker state.
 */
class WakeWordDetector(
    private val context: Context,
    private val threshold: Float = 0.5f,
    private val onDetected: (Float) -> Unit,
    private val onError: (String) -> Unit,
    private val onStateChanged: (String) -> Unit = {}
) {
    private var detector: OpenWakeWord? = null
    @Volatile private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var startedAtMs = 0L
    private var audioCaptureConfirmed = false
    private var lastAudioProgressAtMs = 0L
    private var lastPredictionProgressAtMs = 0L
    private var lastAudioSamples = 0L
    private var lastPredictionCount = 0
    private var lastAudioMetricLogAtMs = 0L
    private var lastReportedState: String? = null

    private val healthCheck = object : Runnable {
        override fun run() {
            if (!isListening) return

            try {
                val engine = detector
                    ?: throw IllegalStateException("El motor de reconocimiento no existe.")

                val threadField = engine.javaClass.getDeclaredField("processingThread").apply {
                    isAccessible = true
                }
                val audioField = engine.javaClass.getDeclaredField("audioRecord").apply {
                    isAccessible = true
                }
                val samplesField = engine.javaClass.getDeclaredField("rawTotalWritten").apply {
                    isAccessible = true
                }
                val predictionsField = engine.javaClass.getDeclaredField("predictionCount").apply {
                    isAccessible = true
                }
                val rawBufferField = engine.javaClass.getDeclaredField("rawBuffer").apply {
                    isAccessible = true
                }
                val rawWritePosField = engine.javaClass.getDeclaredField("rawWritePos").apply {
                    isAccessible = true
                }

                val worker = threadField.get(engine) as? Thread
                val recorder = audioField.get(engine) as? AudioRecord
                val audioSamples = samplesField.getLong(engine)
                val predictionCount = predictionsField.getInt(engine)
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - startedAtMs

                when {
                    worker == null -> fail("El motor no llegó a crear su hilo de procesamiento.")
                    !worker.isAlive -> fail(
                        "El motor de reconocimiento se detuvo durante la inicialización. " +
                            "La biblioteca no comunicó el error original."
                    )
                    recorder == null && elapsed >= STARTUP_TIMEOUT_MS ->
                        fail("El motor no pudo inicializar el micrófono en 45 segundos.")
                    recorder == null -> reportState(STATE_LOADING)
                    recorder.state != AudioRecord.STATE_INITIALIZED ->
                        fail("Android creó el capturador, pero no pudo inicializarlo.")
                    recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING -> {
                        if (!audioCaptureConfirmed) {
                            audioCaptureConfirmed = true
                            lastAudioProgressAtMs = now
                            lastPredictionProgressAtMs = now
                            lastAudioSamples = audioSamples
                            lastPredictionCount = predictionCount
                            reportState(STATE_CAPTURING)
                            Log.i(TAG, "Captura real de audio confirmada.")
                        } else {
                            if (audioSamples > lastAudioSamples) {
                                lastAudioSamples = audioSamples
                                lastAudioProgressAtMs = now
                            }
                            if (predictionCount > lastPredictionCount) {
                                lastPredictionCount = predictionCount
                                lastPredictionProgressAtMs = now
                                Log.d(TAG, "Predicciones procesadas: $predictionCount")
                            }
                            if (now - lastAudioMetricLogAtMs >= AUDIO_METRIC_LOG_INTERVAL_MS) {
                                val rawBuffer = rawBufferField.get(engine) as? FloatArray
                                if (rawBuffer != null && rawBuffer.isNotEmpty()) {
                                    val writePos = rawWritePosField.getInt(engine)
                                    logAudioLevel(rawBuffer, writePos)
                                }
                                lastAudioMetricLogAtMs = now
                            }

                            when {
                                now - lastAudioProgressAtMs >= AUDIO_STALL_TIMEOUT_MS ->
                                    fail("Android mantiene el micrófono abierto, pero no está entregando muestras de audio.")
                                now - lastPredictionProgressAtMs >= MODEL_PROGRESS_TIMEOUT_MS ->
                                    fail("El micrófono recibe audio, pero el modelo no consigue generar predicciones. Requiere revisar el procesamiento ONNX.")
                            }
                        }
                    }
                    audioCaptureConfirmed ->
                        fail("La captura de audio se interrumpió mientras JARVIS escuchaba.")
                    elapsed >= STARTUP_TIMEOUT_MS ->
                        fail("El micrófono no comenzó a capturar audio en 45 segundos.")
                    else -> reportState(STATE_STARTING)
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo verificar el estado del motor", e)
                val elapsed = SystemClock.elapsedRealtime() - startedAtMs
                if (elapsed >= STARTUP_TIMEOUT_MS) {
                    fail("No se pudo verificar el motor de audio: ${e.message ?: e.javaClass.simpleName}")
                    return
                }
                reportState(STATE_CHECKING)
            }

            if (isListening) {
                mainHandler.postDelayed(
                    this,
                    if (audioCaptureConfirmed) HEALTH_CHECK_INTERVAL_MS else STARTUP_CHECK_INTERVAL_MS
                )
            }
        }
    }

    fun start() {
        if (isListening) return

        try {
            reportState(STATE_STARTING)
            startedAtMs = SystemClock.elapsedRealtime()
            audioCaptureConfirmed = false
            lastAudioProgressAtMs = startedAtMs
            lastPredictionProgressAtMs = startedAtMs
            lastAudioSamples = 0L
            lastPredictionCount = 0
            lastAudioMetricLogAtMs = 0L

            detector = OpenWakeWord.Builder(context)
                .setModel(OpenWakeWord.BuiltInModel.HEY_JARVIS)
                .setThreshold(threshold.coerceIn(0.25f, 0.85f))
                .setDebounceMs(2200L)
                .build()

            isListening = true
            detector?.start { score ->
                Log.i(TAG, "Hey Jarvis detectado; score=$score")
                onDetected(score)
            }

            Log.i(TAG, "Motor iniciado; esperando confirmación de captura de audio.")
            mainHandler.removeCallbacks(healthCheck)
            mainHandler.postDelayed(healthCheck, STARTUP_CHECK_INTERVAL_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar detector", e)
            isListening = false
            mainHandler.removeCallbacks(healthCheck)
            onError("Error al iniciar: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Logs only signal-level statistics; audio samples are never stored or transmitted.
     */
    private fun logAudioLevel(buffer: FloatArray, writePos: Int) {
        val sampleCount = minOf(AUDIO_METRIC_SAMPLE_COUNT, buffer.size)
        var sumSquares = 0.0
        var peak = 0f
        for (offset in 0 until sampleCount) {
            val index = (writePos - 1 - offset + buffer.size) % buffer.size
            val sample = kotlin.math.abs(buffer[index])
            if (sample > peak) peak = sample
            sumSquares += sample.toDouble() * sample.toDouble()
        }
        val rms = kotlin.math.sqrt(sumSquares / sampleCount)
        Log.i(TAG, "Nivel de audio PCM16: RMS=%.1f, pico=%.1f".format(rms, peak))
    }

    private fun reportState(state: String) {
        if (lastReportedState == state) return
        lastReportedState = state
        Log.i(TAG, "Estado del motor: $state")
        onStateChanged(state)
    }

    private fun fail(message: String) {
        if (!isListening) return
        Log.e(TAG, message)
        isListening = false
        mainHandler.removeCallbacks(healthCheck)
        onError(message)
    }

    fun stop() {
        mainHandler.removeCallbacks(healthCheck)
        try {
            detector?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error al detener el motor", e)
        }
        isListening = false
        audioCaptureConfirmed = false
        lastReportedState = null
    }

    fun release() {
        mainHandler.removeCallbacks(healthCheck)
        stop()
        try {
            detector?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error al liberar recursos del motor", e)
        }
        detector = null
    }

    fun isListening(): Boolean = isListening

    companion object {
        private const val TAG = "WakeWordDetector"
        private const val STARTUP_TIMEOUT_MS = 45_000L
        private const val STARTUP_CHECK_INTERVAL_MS = 2_000L
        private const val HEALTH_CHECK_INTERVAL_MS = 5_000L
        private const val AUDIO_STALL_TIMEOUT_MS = 15_000L
        private const val MODEL_PROGRESS_TIMEOUT_MS = 30_000L
        private const val AUDIO_METRIC_LOG_INTERVAL_MS = 10_000L
        private const val AUDIO_METRIC_SAMPLE_COUNT = 16_000

        const val STATE_STARTING = "INICIALIZANDO"
        const val STATE_LOADING = "CARGANDO_MODELOS"
        const val STATE_CHECKING = "VERIFICANDO_MOTOR"
        const val STATE_CAPTURING = "CAPTURANDO_AUDIO"
    }
}
