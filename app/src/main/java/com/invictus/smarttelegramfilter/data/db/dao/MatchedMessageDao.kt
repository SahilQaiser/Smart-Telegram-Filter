package com.invictus.smarttelegramfilter.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchedMessageDao {

    @Query("SELECT * FROM matched_messages ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MatchedMessage>>

    @Query("SELECT * FROM matched_messages WHERE channelId = :channelId ORDER BY timestamp DESC")
    fun observeByChannel(channelId: Long): Flow<List<MatchedMessage>>

    @Query("SELECT COUNT(*) FROM matched_messages WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    /** IGNORE conflict so duplicate Telegram messages are silently dropped. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MatchedMessage): Long

    @Query("UPDATE matched_messages SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE matched_messages SET isRead = 1")
    suspend fun markAllRead()

    @Delete
    suspend fun delete(message: MatchedMessage)

    @Query("DELETE FROM matched_messages WHERE timestamp < :cutoffMs")
    suspend fun pruneOlderThan(cutoffMs: Long): Int

    @Query("DELETE FROM matched_messages")
    suspend fun deleteAll()
}
