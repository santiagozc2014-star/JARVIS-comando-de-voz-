package com.jarvis.wakeword

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat

class WakeWordService : Service() {

    private var detector: WakeWordDetector? = null
    private var wakeLock: PowerManager.WakeLock? = null

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
            ACTION_OPEN_CHATGPT -> {
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

        val notification = NotificationHelper.buildListeningNotification(this)

        val fgsType = if (Build.VERSION.SDK_INT >= 34) {
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
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

                // Intento de apertura AUTOMÁTICA de ChatGPT (sin tocar nada)
                val opened = ChatGPTLauncher.launch(this)
                Log.i(TAG, "Apertura automática de ChatGPT: $opened")

                NotificationHelper.showDetectionNotification(this, autoOpened = opened)

                sendBroadcast(Intent(ACTION_STATUS).apply {
                    putExtra(EXTRA_LISTENING, true)
                    putExtra(EXTRA_LAST_SCORE, score)
                    putExtra(EXTRA_AUTO_OPENED, opened)
                })
            },
            onError = { msg ->
                Log.e(TAG, msg)
                NotificationHelper.showErrorNotification(this, msg)
                sendBroadcast(Intent(ACTION_STATUS).apply {
                    putExtra(EXTRA_LISTENING, false)
                    putExtra(EXTRA_ERROR, msg)
                })
                stopSelf()
            }
        )
        detector?.start()

        sendBroadcast(Intent(ACTION_STATUS).apply {
            putExtra(EXTRA_LISTENING, true)
        })
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LISTENING, true)
            .apply()
    }

    private fun stopListening() {
        detector?.release()
        detector = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)

        sendBroadcast(Intent(ACTION_STATUS).apply {
            putExtra(EXTRA_LISTENING, false)
        })
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LISTENING, false)
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
        stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.jarvis.wakeword.STOP"
        const val ACTION_OPEN_CHATGPT = "com.jarvis.wakeword.OPEN_CHATGPT"
        const val ACTION_STATUS = "com.jarvis.wakeword.STATUS"

        const val EXTRA_THRESHOLD = "threshold"
        const val EXTRA_LISTENING = "listening"
        const val EXTRA_LAST_SCORE = "last_score"
        const val EXTRA_AUTO_OPENED = "auto_opened"
        const val EXTRA_ERROR = "error"

        private const val PREFS = "jarvis_prefs"
        private const val KEY_LISTENING = "is_listening"
        private const val TAG = "WakeWordService"

        fun isMarkedListening(context: Context): Boolean {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_LISTENING, false)
        }
    }
}
