package com.invictus.smarttelegramfilter.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.invictus.smarttelegramfilter.MainActivity
import com.invictus.smarttelegramfilter.R
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
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun notifyMatchedMessage(message: MatchedMessage) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.invictus.smarttelegramfilter.OPEN_MESSAGE"
            putExtra("message_id", message.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPi = PendingIntent.getActivity(
            context,
            message.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val telegramUri = buildTelegramDeepLink(
            message.channelUsername, message.channelId, message.telegramMessageId,
        )
        val openTgPi = PendingIntent.getActivity(
            context,
            (message.id + 100_000).toInt(),
            Intent(Intent.ACTION_VIEW, Uri.parse(telegramUri)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        manager.notify(
            nextId.getAndIncrement(),
            NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                .setContentTitle(message.channelName)
                .setContentText(message.textContent.take(120))
                .setSubText("# ${message.matchedKeyword}")
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(openAppPi)
                .addAction(0, "Open in Telegram", openTgPi)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.textContent))
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
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
        )
    }
}

fun buildTelegramDeepLink(channelUsername: String, channelId: Long, messageId: Long): String =
    if (channelUsername.isNotEmpty()) {
        "https://t.me/$channelUsername/$messageId"
    } else {
        val rawId = channelId.toString().removePrefix("-100")
        "tg://privatepost?channel=$rawId&post=$messageId"
    }
