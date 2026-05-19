package com.invictus.smarttelegramfilter.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.invictus.smarttelegramfilter.data.db.dao.ChannelFilterDao
import com.invictus.smarttelegramfilter.data.db.dao.MatchedMessageDao
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilter
import com.invictus.smarttelegramfilter.data.db.entity.Keyword
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage

@Database(
    entities = [ChannelFilter::class, Keyword::class, MatchedMessage::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelFilterDao(): ChannelFilterDao
    abstract fun matchedMessageDao(): MatchedMessageDao
}
