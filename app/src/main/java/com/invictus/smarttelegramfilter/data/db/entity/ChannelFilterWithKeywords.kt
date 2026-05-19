package com.invictus.smarttelegramfilter.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ChannelFilterWithKeywords(
    @Embedded val filter: ChannelFilter,
    @Relation(
        parentColumn = "channelId",
        entityColumn = "channelId",
    )
    val keywords: List<Keyword>,
)
