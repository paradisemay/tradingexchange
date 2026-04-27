package com.tradingexchange.app.data.remote

import com.tradingexchange.app.data.local.TokenStore
import com.tradingexchange.app.domain.model.TokenPair
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @Named("authApi") private val authApi: BrokerApi,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? = runBlocking {
        if (responseCount(response) >= 2) return@runBlocking null
        mutex.withLock {
            val currentRefresh = tokenStore.refreshToken.first() ?: return@withLock null
            val tokenPair = runCatching {
                authApi.refresh(RefreshRequestDto(currentRefresh)).let { TokenPair(it.accessToken, it.refreshToken) }
            }.getOrNull()
            if (tokenPair == null) {
                tokenStore.clear()
                return@withLock null
            }
            tokenStore.save(tokenPair)
            response.request.newBuilder()
                .header("Authorization", "Bearer ${tokenPair.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
