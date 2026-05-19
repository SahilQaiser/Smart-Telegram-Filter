package com.invictus.smarttelegramfilter.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invictus.smarttelegramfilter.data.db.entity.MatchedMessage
import com.invictus.smarttelegramfilter.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repo: MessageRepository,
) : ViewModel() {

    val messages: StateFlow<List<MatchedMessage>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = repo.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markRead(id: Long) = viewModelScope.launch { repo.markRead(id) }

    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }

    fun delete(message: MatchedMessage) = viewModelScope.launch { repo.delete(message) }

    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }
}
