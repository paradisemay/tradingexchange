package com.tradingexchange.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tradingexchange.app.domain.model.TokenPair
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tokenDataStore by preferencesDataStore("auth_tokens")

@Singleton
class TokenStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    val accessToken: Flow<String?> = context.tokenDataStore.data.map { it[accessKey] }
    val refreshToken: Flow<String?> = context.tokenDataStore.data.map { it[refreshKey] }
    val isLoggedIn: Flow<Boolean> = accessToken.map { !it.isNullOrBlank() }

    suspend fun save(tokens: TokenPair) {
        context.tokenDataStore.edit {
            it[accessKey] = tokens.accessToken
            it[refreshKey] = tokens.refreshToken
        }
    }

    suspend fun clear() {
        context.tokenDataStore.edit { it.clear() }
    }
}
