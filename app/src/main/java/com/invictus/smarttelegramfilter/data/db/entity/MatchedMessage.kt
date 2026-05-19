package com.invictus.smarttelegramfilter.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matched_messages",
    // Telegram message IDs are unique per chat, so (channelId, telegramMessageId) is globally unique.
    indices = [Index(value = ["channelId", "telegramMessageId"], unique = true)],
)
data class MatchedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val telegramMessageId: Long,
    val channelId: Long,
    val channelName: String,
    val channelUsername: String = "",
    val senderName: String,
    val textContent: String,
    val matchedKeyword: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
)
