package com.tradingexchange.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingexchange.app.data.repository.RepositoryException
import com.tradingexchange.app.domain.model.UserProfile
import com.tradingexchange.app.domain.model.userMessage
import com.tradingexchange.app.domain.repository.BrokerRepository
import com.tradingexchange.app.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(val isLoading: Boolean = false, val profile: UserProfile? = null, val error: String? = null)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val brokerRepository: BrokerRepository,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isLoading = true)
            runCatching { brokerRepository.getProfile() }
                .onSuccess { mutableState.value = ProfileUiState(profile = it) }
                .onFailure {
                    val message = (it as? RepositoryException)?.appError?.userMessage() ?: "Не удалось загрузить профиль"
                    mutableState.value = ProfileUiState(error = message)
                }
        }
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
