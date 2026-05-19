package com.invictus.smarttelegramfilter.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel_filters")
data class ChannelFilter(
    @PrimaryKey val channelId: Long,
    val channelName: String,
    val channelHandle: String = "",
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
)
