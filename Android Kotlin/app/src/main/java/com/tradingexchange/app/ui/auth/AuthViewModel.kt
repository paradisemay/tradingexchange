package com.tradingexchange.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingexchange.app.data.repository.RepositoryException
import com.tradingexchange.app.domain.model.userMessage
import com.tradingexchange.app.domain.usecase.LoginUseCase
import com.tradingexchange.app.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "trader@example.com",
    val password: String = "secret123",
    val fullName: String = "Ivan Ivanov",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    fun updateEmail(value: String) = update { copy(email = value, error = null) }
    fun updatePassword(value: String) = update { copy(password = value, error = null) }
    fun updateFullName(value: String) = update { copy(fullName = value, error = null) }
    fun toggleMode() = update { copy(isRegisterMode = !isRegisterMode, error = null) }

    fun submit() {
        val current = state.value
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            runCatching {
                if (current.isRegisterMode) {
                    registerUseCase(current.email, current.password, current.fullName)
                } else {
                    loginUseCase(current.email, current.password)
                }
            }.onFailure { error ->
                val message = (error as? RepositoryException)?.appError?.userMessage() ?: error.message ?: "Ошибка входа"
                update { copy(error = message) }
            }
            update { copy(isLoading = false) }
        }
    }

    private fun update(reducer: AuthUiState.() -> AuthUiState) {
        mutableState.value = mutableState.value.reducer()
    }
}
