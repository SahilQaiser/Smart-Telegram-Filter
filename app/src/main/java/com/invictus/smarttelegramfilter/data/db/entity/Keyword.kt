package com.invictus.smarttelegramfilter.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "keywords",
    foreignKeys = [
        ForeignKey(
            entity = ChannelFilter::class,
            parentColumns = ["channelId"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("channelId")],
)
data class Keyword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    /** Literal string or regex pattern. Prefixed with "r/" in the UI to indicate regex. */
    val pattern: String,
    val isRegex: Boolean = false,
)
