package com.jarvis.wakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_ID = "jarvis_wakeword_channel"
    const val DETECTION_CHANNEL_ID = "jarvis_detection_channel"
    const val NOTIFICATION_ID = 1001
    const val DETECTION_NOTIFICATION_ID = 1002

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val listeningChannel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS escuchando",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Escucha continua de «Hey Jarvis». El micrófono está activo."
                setShowBadge(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(listeningChannel)

            val detectionChannel = NotificationChannel(
                DETECTION_CHANNEL_ID,
                "Detección de JARVIS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso cuando se detecta la frase Hey Jarvis."
                setShowBadge(false)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 120, 80, 120)
            }
            manager.createNotificationChannel(detectionChannel)
        }
    }

    fun buildListeningNotification(context: Context): Notification {
        val stopIntent = Intent(context, WakeWordService::class.java).apply {
            action = WakeWordService.ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openApp = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 1, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("JARVIS escuchando")
            .setContentText("Di «Hey Jarvis» · Micrófono activo")
            .setSmallIcon(R.drawable.ic_jarvis)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()
    }

    fun showDetectionNotification(context: Context, autoOpened: Boolean) {
        val openChatGpt = Intent(context, MainActivity::class.java).apply {
            action = WakeWordService.ACTION_OPEN_CHATGPT
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pending = PendingIntent.getActivity(
            context, 2, openChatGpt,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (autoOpened) "Hey Jarvis → ChatGPT abierto" else "Hey Jarvis detectado"
        val text = if (autoOpened) "Ya puedes hablar con ChatGPT" else "Toca para abrir ChatGPT"

        val builder = NotificationCompat.Builder(context, DETECTION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_jarvis)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (!autoOpened) {
            builder.setFullScreenIntent(pending, true)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(DETECTION_NOTIFICATION_ID, builder.build())
    }

    fun showErrorNotification(context: Context, message: String) {
        val openApp = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 3, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("JARVIS no puede escuchar")
            .setContentText(message.take(120))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(DETECTION_NOTIFICATION_ID, notification)
    }
}
