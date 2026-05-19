package com.invictus.smarttelegramfilter.data.repository

import com.invictus.smarttelegramfilter.data.db.dao.MatchedMessageDao
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(private val dao: MatchedMessageDao) {

    fun observeAll(): Flow<List<MatchedMessage>> = dao.observeAll()

    fun observeByChannel(channelId: Long): Flow<List<MatchedMessage>> =
        dao.observeByChannel(channelId)

    fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    suspend fun insert(message: MatchedMessage): Long = dao.insert(message)

    suspend fun markRead(id: Long) = dao.markRead(id)

    suspend fun markAllRead() = dao.markAllRead()

    suspend fun delete(message: MatchedMessage) = dao.delete(message)

    suspend fun pruneOlderThan30Days() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        dao.pruneOlderThan(cutoff)
    }
}
