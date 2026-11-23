package com.jarvis.wakeword

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import java.util.concurrent.atomic.AtomicLong

class WakeWordService : Service() {

    private var detector: WakeWordDetector? = null
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile private var terminalError = false
    private val launchGeneration = AtomicLong(0L)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopListening()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val threshold = intent?.getFloatExtra(EXTRA_THRESHOLD, 0.5f) ?: 0.5f
                startListening(threshold)
            }
        }
        return START_STICKY
    }

    private fun startListening(threshold: Float) {
        if (detector?.isListening() == true) {
            Log.i(TAG, "Ya estaba escuchando")
            return
        }

        terminalError = false
        val sessionGeneration = launchGeneration.incrementAndGet()
        val notification = NotificationHelper.buildListeningNotification(this)

        // Android 11 (API 30) requires the microphone FGS type in the
        // startForeground call when targetSdk is 30 or higher.
        val fgsType = if (ForegroundServiceTypePolicy.requiresExplicitMicrophoneType(Build.VERSION.SDK_INT)) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID,
            notification,
            fgsType
        )

        acquireWakeLock()

        detector = WakeWordDetector(
            context = this,
            threshold = threshold,
            onDetected = { score ->
                Log.i(TAG, "Detección con score $score")
                vibrateShort()

                // Publish the user-controlled fallback before attempting an
                // automatic launch. Android may silently block background
                // activity starts, so this notification must already exist.
                try {
                    NotificationHelper.showDetectionNotification(this)
                } catch (e: Exception) {
                    // A notification failure must not suppress the automatic
                    // launch attempt; the two recovery paths are independent.
                    Log.w(TAG, "No se pudo publicar la notificación de detección", e)
                }

                // The detector callback runs on its processing worker. Move
                // activity-launch work to the main thread; this does not bypass
                // Android's background-launch policy, but avoids doing UI work
                // on the audio inference thread.
                mainHandler.post {
                    // Ignore delayed detections after stop/error/restart. Without
                    // this generation check, a queued callback could open ChatGPT
                    // after the user had already stopped JARVIS.
                    if (!WakeWordLaunchGate.canLaunch(
                            detectedGeneration = sessionGeneration,
                            currentGeneration = launchGeneration.get(),
                            terminalError = terminalError
                        )
                    ) {
                        Log.i(TAG, "Apertura automática cancelada: sesión ya no activa")
                        return@post
                    }

                    val launchResult = ChatGPTLauncher.requestVoiceAssistantLaunch(this)
                    Log.i(TAG, "Resultado de solicitud de apertura: $launchResult")

                    sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).apply {
                        putExtra(EXTRA_LISTENING, true)
                        putExtra(EXTRA_ENGINE_STATE, WakeWordDetector.STATE_CAPTURING)
                        putExtra(EXTRA_LAST_SCORE, score)
                        putExtra(EXTRA_LAUNCH_RESULT, launchResult.name)
                    })
                }
            },
            onError = { msg ->
                terminalError = true
                launchGeneration.incrementAndGet()
                Log.e(TAG, msg)
                NotificationHelper.showErrorNotification(this, msg)
                WakeWordRuntimeState.setListening(false)
                getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_ENGINE_STATE, STATE_ERROR)
                    .apply()
                sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).apply {
                    putExtra(EXTRA_LISTENING, false)
                    putExtra(EXTRA_ENGINE_STATE, STATE_ERROR)
                    putExtra(EXTRA_ERROR, msg)
                })
                stopSelf()
            },
            onStateChanged = { state ->
                getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_ENGINE_STATE, state)
                    .apply()
                NotificationHelper.updateListeningNotification(this, state)
                sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).apply {
                    putExtra(EXTRA_LISTENING, true)
                    putExtra(EXTRA_ENGINE_STATE, state)
                })
            }
        )
        detector?.start()

        // start() can report a synchronous initialization failure through onError.
        // Do not overwrite that stopped/error state with a stale "listening" status.
        if (detector?.isListening() != true) {
            launchGeneration.incrementAndGet()
            Log.e(TAG, "El detector no pudo iniciar; se evita publicar un estado de escucha activo.")
            return
        }

        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).apply {
            putExtra(EXTRA_LISTENING, true)
            putExtra(EXTRA_ENGINE_STATE, WakeWordDetector.STATE_STARTING)
        })
        WakeWordRuntimeState.setListening(true)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENGINE_STATE, WakeWordDetector.STATE_STARTING)
            .apply()
    }

    private fun stopListening() {
        // Invalidate any launch callback already queued on the main thread.
        launchGeneration.incrementAndGet()
        detector?.release()
        detector = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)

        WakeWordRuntimeState.setListening(false)
        val finalState = if (terminalError) STATE_ERROR else STATE_STOPPED
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).apply {
            putExtra(EXTRA_LISTENING, false)
            putExtra(EXTRA_ENGINE_STATE, finalState)
        })
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENGINE_STATE, finalState)
            .apply()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "JARVIS::WakeWordLock"
            ).apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L)
            }
            Log.i(TAG, "WakeLock adquirido")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo adquirir WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando WakeLock", e)
        }
        wakeLock = null
    }

    private fun vibrateShort() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibración no disponible", e)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.jarvis.wakeword.STOP"
        const val ACTION_STATUS = "com.jarvis.wakeword.STATUS"

        const val EXTRA_THRESHOLD = "threshold"
        const val EXTRA_LISTENING = "listening"
        const val EXTRA_LAST_SCORE = "last_score"
        const val EXTRA_LAUNCH_RESULT = "launch_result"
        const val EXTRA_ERROR = "error"
        const val EXTRA_ENGINE_STATE = "engine_state"
        const val STATE_ERROR = "ERROR"
        const val STATE_STOPPED = "DETENIDO"

        private const val PREFS = "jarvis_prefs"
        private const val KEY_ENGINE_STATE = "engine_state"
        private const val TAG = "WakeWordService"

        fun isMarkedListening(): Boolean = WakeWordRuntimeState.isListening()

        fun getMarkedEngineState(context: Context): String {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ENGINE_STATE, STATE_STOPPED) ?: STATE_STOPPED
        }
    }
}
