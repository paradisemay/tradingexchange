package com.tradingexchange.app.core.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tradingexchange.app.BuildConfig
import com.tradingexchange.app.data.local.AppDatabase
import com.tradingexchange.app.data.local.BrokerDao
import com.tradingexchange.app.data.remote.AuthInterceptor
import com.tradingexchange.app.data.remote.BrokerApi
import com.tradingexchange.app.data.remote.RemoteErrorMapper
import com.tradingexchange.app.data.remote.TokenAuthenticator
import com.tradingexchange.app.data.repository.AuthRepositoryImpl
import com.tradingexchange.app.data.repository.BrokerRepositoryImpl
import com.tradingexchange.app.data.websocket.QuotesRepositoryImpl
import com.tradingexchange.app.domain.repository.AuthRepository
import com.tradingexchange.app.domain.repository.BrokerRepository
import com.tradingexchange.app.domain.repository.QuotesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "trading_exchange.db").build()

    @Provides
    fun provideBrokerDao(database: AppDatabase): BrokerDao = database.brokerDao()

    @Provides
    @Singleton
    @Named("authClient")
    fun provideAuthClient(): OkHttpClient =
        baseHttpClient()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

    @Provides
    @Singleton
    @Named("mainClient")
    fun provideMainClient(authInterceptor: AuthInterceptor, authenticator: TokenAuthenticator): OkHttpClient =
        baseHttpClient()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

    @Provides
    @Singleton
    @Named("authApi")
    fun provideAuthApi(@Named("authClient") client: OkHttpClient, json: Json): BrokerApi =
        retrofit(client, json).create(BrokerApi::class.java)

    @Provides
    @Singleton
    fun provideBrokerApi(@Named("mainClient") client: OkHttpClient, json: Json): BrokerApi =
        retrofit(client, json).create(BrokerApi::class.java)

    @Provides
    @Singleton
    fun provideErrorMapper(json: Json): RemoteErrorMapper = RemoteErrorMapper(json)

    @Provides
    @Singleton
    @Named("wsUrl")
    fun provideWsUrl(): String = BuildConfig.WS_BASE_URL

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    private fun baseHttpClient(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindBrokerRepository(impl: BrokerRepositoryImpl): BrokerRepository

    @Binds
    abstract fun bindQuotesRepository(impl: QuotesRepositoryImpl): QuotesRepository
}
