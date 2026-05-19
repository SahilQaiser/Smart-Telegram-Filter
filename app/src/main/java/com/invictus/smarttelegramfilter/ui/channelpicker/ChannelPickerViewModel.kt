package com.invictus.smarttelegramfilter.ui.channelpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilter
import com.invictus.smarttelegramfilter.data.repository.FilterRepository
import com.invictus.smarttelegramfilter.telegram.TdlibClient
import com.invictus.smarttelegramfilter.telegram.TdlibException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

sealed interface ChannelPickerUiState {
    data object Loading : ChannelPickerUiState
    data class Error(val message: String) : ChannelPickerUiState
    data object Success : ChannelPickerUiState
}

@HiltViewModel
class ChannelPickerViewModel @Inject constructor(
    private val tdlib: TdlibClient,
    private val filterRepository: FilterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChannelPickerUiState>(ChannelPickerUiState.Loading)
    val uiState: StateFlow<ChannelPickerUiState> = _uiState

    val searchQuery = MutableStateFlow("")

    private val _allChannels = MutableStateFlow<List<SubscribedChannel>>(emptyList())

    val trackedChannelIds: StateFlow<Set<Long>> = filterRepository.observeAllChannels()
        .map { list -> list.map { it.filter.channelId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val filteredChannels: StateFlow<List<SubscribedChannel>> =
        combine(_allChannels, searchQuery, trackedChannelIds) { channels, query, tracked ->
            val withTracking = channels.map { it.copy(isAlreadyTracked = it.id in tracked) }
            if (query.isBlank()) withTracking
            else withTracking.filter { it.title.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent: SharedFlow<Unit> = _navigationEvent.asSharedFlow()

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _uiState.value = ChannelPickerUiState.Loading
            try {
                // Build list of all chat lists: main, archive, plus every folder
                val chatLists = buildList<TdApi.ChatList> {
                    add(TdApi.ChatListMain())
                    add(TdApi.ChatListArchive())
                    tdlib.chatFolderIds.value.forEach { add(TdApi.ChatListFolder(it)) }
                }

                // Drain every list until TDLib returns 404 (exhausted)
                for (list in chatLists) {
                    while (true) {
                        try {
                            tdlib.send(TdApi.LoadChats(list, 100))
                        } catch (e: TdlibException) {
                            if (e.code == 404) break
                            throw e
                        }
                    }
                }

                // Collect IDs from all lists, dedup
                val allIds = mutableSetOf<Long>()
                for (list in chatLists) {
                    runCatching {
                        tdlib.send(TdApi.GetChats(list, Int.MAX_VALUE)).chatIds.forEach { allIds.add(it) }
                    }
                }
                val chatIds = allIds.toLongArray()

                val chats = chatIds.map { id ->
                    async { runCatching { tdlib.send(TdApi.GetChat(id)) }.getOrNull() }
                }.awaitAll().filterNotNull()

                val channelChats = chats.filter { chat ->
                    (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == true
                }

                val channels = channelChats.map { chat ->
                    async {
                        val supergroupId = (chat.type as TdApi.ChatTypeSupergroup).supergroupId
                        val info = runCatching { tdlib.send(TdApi.GetSupergroup(supergroupId)) }.getOrNull()
                        SubscribedChannel(
                            id = chat.id,
                            title = chat.title,
                            username = info?.usernames?.editableUsername ?: "",
                            memberCount = info?.memberCount ?: 0,
                            isAlreadyTracked = false,
                        )
                    }
                }.awaitAll().sortedBy { it.title }

                _allChannels.value = channels
                _uiState.value = ChannelPickerUiState.Success
            } catch (e: Exception) {
                _uiState.value = ChannelPickerUiState.Error(e.message ?: "Failed to load channels")
            }
        }
    }

    fun selectChannel(channel: SubscribedChannel) {
        viewModelScope.launch {
            filterRepository.addOrUpdateChannel(
                ChannelFilter(
                    channelId = channel.id,
                    channelName = channel.title,
                    channelHandle = channel.username,
                    isActive = true,
                )
            )
            _navigationEvent.emit(Unit)
        }
    }

    fun retry() = loadChannels()
}
