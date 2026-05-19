package com.invictus.smarttelegramfilter.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilter
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilterWithKeywords
import com.invictus.smarttelegramfilter.data.db.entity.Keyword
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelFilterDao {

    // ── Channels ──────────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM channel_filters ORDER BY addedAt DESC")
    fun observeAllWithKeywords(): Flow<List<ChannelFilterWithKeywords>>

    @Transaction
    @Query("SELECT * FROM channel_filters WHERE isActive = 1")
    fun observeActiveWithKeywords(): Flow<List<ChannelFilterWithKeywords>>

    @Query("SELECT * FROM channel_filters WHERE isActive = 1")
    suspend fun getActiveFilters(): List<ChannelFilter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFilter(filter: ChannelFilter)

    @Delete
    suspend fun deleteFilter(filter: ChannelFilter)

    @Query("UPDATE channel_filters SET isActive = :active WHERE channelId = :channelId")
    suspend fun setActive(channelId: Long, active: Boolean)

    // ── Keywords ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM keywords WHERE channelId = :channelId")
    fun observeKeywords(channelId: Long): Flow<List<Keyword>>

    /** Used by the matching engine at startup and on filter change. */
    @Query(
        """
        SELECT k.* FROM keywords k
        INNER JOIN channel_filters cf ON k.channelId = cf.channelId
        WHERE cf.isActive = 1
        """
    )
    suspend fun getActiveKeywords(): List<Keyword>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKeyword(keyword: Keyword)

    @Delete
    suspend fun deleteKeyword(keyword: Keyword)

    @Query("DELETE FROM keywords WHERE channelId = :channelId")
    suspend fun clearKeywords(channelId: Long)
}
