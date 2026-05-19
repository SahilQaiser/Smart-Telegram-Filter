package com.invictus.smarttelegramfilter.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.invictus.smarttelegramfilter.MainActivity
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val SERVICE_NOTIFICATION_ID = 1
        private const val AUTH_NOTIFICATION_ID = 2
        private const val CHANNEL_SERVICE  = "stf_service"
        private const val CHANNEL_MESSAGES = "stf_messages"
    }

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val nextId = AtomicInteger(1000)

    init {
        createChannels()
    }

    private fun createChannels() {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                "Filter Service",
                NotificationManager.IMPORTANCE_MIN,
            ).apply { description = "Persistent background indicator" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Matched Messages",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Alerts for keyword-matched Telegram messages" }
        )
    }

    fun buildServiceNotification(): Notification =
        NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setContentTitle("Smart Filter Active")
            .setContentText("Monitoring selected channels for keywords")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun notifyMatchedMessage(message: MatchedMessage) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.invictus.smarttelegramfilter.OPEN_MESSAGE"
            putExtra("message_id", message.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            message.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        manager.notify(
            nextId.getAndIncrement(),
            NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setContentTitle(message.channelName)
                .setContentText(message.textContent.take(120))
                .setSubText("Keyword: ${message.matchedKeyword}")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
        )
    }

    fun notifyAuthRequired() {
        val pi = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            AUTH_NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setContentTitle("Telegram login required")
                .setContentText("Tap to sign in to your Telegram account")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
        )
    }
}
