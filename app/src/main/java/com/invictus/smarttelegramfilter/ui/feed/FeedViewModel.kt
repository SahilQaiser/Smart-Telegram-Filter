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

    val searchQuery       = MutableStateFlow("")
    val selectedChannelId = MutableStateFlow<Long?>(null)
    val showStarredOnly   = MutableStateFlow(false)

    private val allMessages = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages: StateFlow<List<MatchedMessage>> =
        combine(allMessages, searchQuery, selectedChannelId, showStarredOnly) { msgs, q, chId, starred ->
            var r = msgs
            if (chId != null)   r = r.filter { it.channelId == chId }
            if (starred)        r = r.filter { it.isStarred }
            if (q.isNotBlank()) {
                val lower = q.lowercase()
                r = r.filter {
                    it.textContent.contains(lower, ignoreCase = true) ||
                    it.channelName.contains(lower, ignoreCase = true) ||
                    it.matchedKeyword.contains(lower, ignoreCase = true) ||
                    it.senderName.contains(lower, ignoreCase = true)
                }
            }
            r
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasMessages: StateFlow<Boolean> = allMessages
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val availableChannels: StateFlow<List<Pair<Long, String>>> = allMessages
        .map { msgs -> msgs.map { it.channelId to it.channelName }.distinctBy { it.first } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = repo.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val starredCount: StateFlow<Int> = repo.observeStarredCount()
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

    fun selectChannel(id: Long?) { selectedChannelId.value = id }

    fun markRead(id: Long) = viewModelScope.launch { repo.markRead(id) }
    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }
    fun archive(message: MatchedMessage) = viewModelScope.launch { repo.archive(message.id) }
    fun unarchive(message: MatchedMessage) = viewModelScope.launch { repo.unarchive(message.id) }
    fun delete(message: MatchedMessage) = viewModelScope.launch { repo.delete(message) }
    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }
    fun clearArchive() = viewModelScope.launch { repo.clearArchive() }
    fun toggleStar(message: MatchedMessage) = viewModelScope.launch { repo.setStar(message.id, !message.isStarred) }
}
