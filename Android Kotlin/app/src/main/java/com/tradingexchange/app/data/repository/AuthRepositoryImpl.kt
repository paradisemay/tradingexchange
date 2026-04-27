package com.tradingexchange.app.data.repository

import com.tradingexchange.app.data.local.TokenStore
import com.tradingexchange.app.data.remote.BrokerApi
import com.tradingexchange.app.data.remote.LoginRequestDto
import com.tradingexchange.app.data.remote.LogoutRequestDto
import com.tradingexchange.app.data.remote.RegisterRequestDto
import com.tradingexchange.app.data.remote.toDomain
import com.tradingexchange.app.domain.model.TokenPair
import com.tradingexchange.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: BrokerApi,
    private val tokenStore: TokenStore,
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = tokenStore.isLoggedIn

    override suspend fun register(email: String, password: String, fullName: String): TokenPair {
        val tokens = api.register(RegisterRequestDto(email, password, fullName)).toDomain()
        tokenStore.save(tokens)
        return tokens
    }

    override suspend fun login(email: String, password: String): TokenPair {
        val tokens = api.login(LoginRequestDto(email, password)).toDomain()
        tokenStore.save(tokens)
        return tokens
    }

    override suspend fun logout() {
        val refresh = tokenStore.refreshToken.first()
        if (!refresh.isNullOrBlank()) {
            runCatching { api.logout(LogoutRequestDto(refresh)) }
        }
        tokenStore.clear()
    }
}
