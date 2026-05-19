package com.invictus.smarttelegramfilter.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invictus.smarttelegramfilter.telegram.TdlibClient
import com.invictus.smarttelegramfilter.telegram.TelegramService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tdlib: TdlibClient,
) : ViewModel() {

    val authState = TelegramService.authState.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null,
    )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun submitPhone(phone: String) = launch {
        tdlib.send(TdApi.SetAuthenticationPhoneNumber(phone, null))
    }

    fun submitCode(code: String) = launch {
        tdlib.send(TdApi.CheckAuthenticationCode(code))
    }

    fun submitPassword(password: String) = launch {
        tdlib.send(TdApi.CheckAuthenticationPassword(password))
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { block() }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
