package com.invictus.smarttelegramfilter.data.repository

import com.invictus.smarttelegramfilter.data.db.dao.ChannelFilterDao
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilter
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilterWithKeywords
import com.invictus.smarttelegramfilter.data.db.entity.Keyword
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(private val dao: ChannelFilterDao) {

    fun observeAllChannels(): Flow<List<ChannelFilterWithKeywords>> =
        dao.observeAllWithKeywords()

    fun observeActiveChannels(): Flow<List<ChannelFilterWithKeywords>> =
        dao.observeActiveWithKeywords()

    suspend fun addOrUpdateChannel(filter: ChannelFilter) = dao.upsertFilter(filter)

    suspend fun removeChannel(filter: ChannelFilter) = dao.deleteFilter(filter)

    suspend fun setChannelActive(channelId: Long, active: Boolean) =
        dao.setActive(channelId, active)

    fun observeKeywords(channelId: Long): Flow<List<Keyword>> =
        dao.observeKeywords(channelId)

    /** Replaces all keywords for a channel atomically. */
    suspend fun setKeywords(channelId: Long, keywords: List<Keyword>) {
        dao.clearKeywords(channelId)
        keywords.forEach { dao.upsertKeyword(it) }
    }

    suspend fun addKeyword(keyword: Keyword) = dao.upsertKeyword(keyword)

    suspend fun removeKeyword(keyword: Keyword) = dao.deleteKeyword(keyword)

    suspend fun getActiveKeywords() = dao.getActiveKeywords()
}
