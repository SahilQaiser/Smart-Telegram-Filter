package com.invictus.smarttelegramfilter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invictus.smarttelegramfilter.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {

    val quietEnabled: StateFlow<Boolean> = repo.quietEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val quietStart: StateFlow<Int> = repo.quietStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 22)

    val quietEnd: StateFlow<Int> = repo.quietEnd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 8)

    fun setQuietEnabled(v: Boolean) = viewModelScope.launch { repo.setQuietEnabled(v) }
    fun setQuietStart(h: Int)       = viewModelScope.launch { repo.setQuietStart(h) }
    fun setQuietEnd(h: Int)         = viewModelScope.launch { repo.setQuietEnd(h) }
}
