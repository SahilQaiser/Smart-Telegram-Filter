package com.invictus.smarttelegramfilter.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilter
import com.invictus.smarttelegramfilter.data.db.entity.ChannelFilterWithKeywords
import com.invictus.smarttelegramfilter.data.db.entity.Keyword
import com.invictus.smarttelegramfilter.data.repository.FilterRepository
import com.invictus.smarttelegramfilter.telegram.TdlibClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val filterRepo: FilterRepository,
    private val tdlib: TdlibClient,
) : ViewModel() {

    val channels: StateFlow<List<ChannelFilterWithKeywords>> = filterRepo.observeAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    // ── Channel management ────────────────────────────────────────────────────

    fun addChannelByHandle(handle: String) = viewModelScope.launch {
        if (handle.isBlank()) return@launch
        _isLoading.value = true
        runCatching {
            val chat = tdlib.send(TdApi.SearchPublicChat(handle.removePrefix("@").trim()))
            filterRepo.addOrUpdateChannel(
                ChannelFilter(
                    channelId     = chat.id,
                    channelName   = chat.title,
                    channelHandle = handle.removePrefix("@").trim(),
                )
            )
        }.onFailure { _snackbar.emit("Could not find \"$handle\": ${it.message}") }
        _isLoading.value = false
    }

    fun removeChannel(filter: ChannelFilter) = viewModelScope.launch {
        filterRepo.removeChannel(filter)
    }

    fun toggleActive(channelId: Long, currentlyActive: Boolean) = viewModelScope.launch {
        filterRepo.setChannelActive(channelId, !currentlyActive)
    }

    // ── Keyword management ────────────────────────────────────────────────────

    /**
     * Parses a comma-separated keyword string and replaces the channel's keyword set.
     * Prefix a pattern with "r/" to treat it as case-insensitive regex.
     * Example: "bitcoin, r/eth(ereum)?, NFT"
     */
    fun setKeywordsFromCsv(channelId: Long, csv: String) = viewModelScope.launch {
        val keywords = csv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { raw ->
                val isRegex = raw.startsWith("r/", ignoreCase = true)
                Keyword(
                    channelId = channelId,
                    pattern   = if (isRegex) raw.drop(2) else raw,
                    isRegex   = isRegex,
                )
            }
        filterRepo.setKeywords(channelId, keywords)
    }
}
