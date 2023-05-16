package com.company.fifcorex_toma_de_pedidos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.company.fifcorex_toma_de_pedidos.R

abstract class OfflineBaseWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    protected val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * To send notification, Oreo and later versions (API 26+) require a notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OFFLINE_NOTIFICATION_CHANNEL_ID,
                OFFLINE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    protected fun createNotification(
            maxStep: Int = 100,
            currentStep: Int = 0,
            pendingIntent: PendingIntent? = null
    ): Notification {
        createNotificationChannel()
        val builder = NotificationCompat.Builder(applicationContext, OFFLINE_NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Syncing offline data...")
                        .setSmallIcon(R.drawable.ic_sync)
                        .setWhen(System.currentTimeMillis())
                        .setOngoing(true)
                        .setProgress(maxStep, currentStep, false)

        pendingIntent?.also {
            builder.setContentIntent(it)
        }

        return builder.build()
    }

    companion object {
        const val OFFLINE_NOTIFICATION_CHANNEL_ID = "offline_wizard_channel"
        const val OFFLINE_NOTIFICATION_CHANNEL_NAME = "SAP Wizard Channel"
        const val OFFLINE_NOTIFICATION_CHANNEL_INT_ID = 1
    }
}