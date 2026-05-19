package com.invictus.smarttelegramfilter.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage
import kotlinx.coroutines.flow.Flow

data class ChannelMatchCount(val channelId: Long, val count: Int)

@Dao
interface MatchedMessageDao {

    @Query("SELECT * FROM matched_messages WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MatchedMessage>>

    @Query("SELECT * FROM matched_messages WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun observeArchived(): Flow<List<MatchedMessage>>

    @Query("SELECT COUNT(*) FROM matched_messages WHERE isArchived = 1")
    fun observeArchivedCount(): Flow<Int>

    @Query("SELECT * FROM matched_messages WHERE channelId = :channelId AND isArchived = 0 ORDER BY timestamp DESC")
    fun observeByChannel(channelId: Long): Flow<List<MatchedMessage>>

    @Query("SELECT COUNT(*) FROM matched_messages WHERE isRead = 0 AND isArchived = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT channelId, COUNT(*) as count FROM matched_messages WHERE isArchived = 0 GROUP BY channelId")
    fun observeMatchCountByChannel(): Flow<List<ChannelMatchCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MatchedMessage): Long

    @Query("UPDATE matched_messages SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE matched_messages SET isRead = 1 WHERE isArchived = 0")
    suspend fun markAllRead()

    @Query("UPDATE matched_messages SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE matched_messages SET isArchived = 0 WHERE id = :id")
    suspend fun unarchive(id: Long)

    @Delete
    suspend fun delete(message: MatchedMessage)

    @Query("DELETE FROM matched_messages WHERE isArchived = 0")
    suspend fun deleteAll()

    @Query("DELETE FROM matched_messages WHERE isArchived = 1")
    suspend fun clearArchive()

    @Query("DELETE FROM matched_messages WHERE timestamp < :cutoffMs")
    suspend fun pruneOlderThan(cutoffMs: Long): Int
}
