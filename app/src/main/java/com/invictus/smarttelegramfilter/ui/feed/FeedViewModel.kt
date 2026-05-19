package com.invictus.smarttelegramfilter.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage
import com.invictus.smarttelegramfilter.data.repository.FilterRepository
import com.invictus.smarttelegramfilter.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repo: MessageRepository,
    private val filterRepo: FilterRepository,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    private val allMessages = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages: StateFlow<List<MatchedMessage>> = combine(allMessages, searchQuery) { msgs, q ->
        if (q.isBlank()) msgs
        else {
            val lower = q.lowercase()
            msgs.filter { msg ->
                msg.textContent.contains(lower, ignoreCase = true) ||
                msg.channelName.contains(lower, ignoreCase = true) ||
                msg.matchedKeyword.contains(lower, ignoreCase = true) ||
                msg.senderName.contains(lower, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasMessages: StateFlow<Boolean> = allMessages
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val unreadCount: StateFlow<Int> = repo.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val archivedMessages: StateFlow<List<MatchedMessage>> = repo.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedCount: StateFlow<Int> = repo.observeArchivedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val channelKeywords: StateFlow<Map<Long, List<String>>> = filterRepo.observeAllChannels()
        .map { list ->
            list.associate { entry ->
                entry.filter.channelId to entry.keywords.map { it.pattern }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun markRead(id: Long) = viewModelScope.launch { repo.markRead(id) }
    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }
    fun archive(message: MatchedMessage) = viewModelScope.launch { repo.archive(message.id) }
    fun unarchive(message: MatchedMessage) = viewModelScope.launch { repo.unarchive(message.id) }
    fun delete(message: MatchedMessage) = viewModelScope.launch { repo.delete(message) }
    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }
    fun clearArchive() = viewModelScope.launch { repo.clearArchive() }
}
