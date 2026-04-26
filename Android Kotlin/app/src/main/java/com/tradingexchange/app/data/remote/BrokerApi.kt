package com.tradingexchange.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BrokerApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): RegisterResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenPairDto

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenPairDto

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto)

    @GET("api/v1/me")
    suspend fun me(): UserProfileDto

    @GET("api/v1/portfolio")
    suspend fun portfolio(): PortfolioResponseDto

    @GET("api/v1/instruments")
    suspend fun instruments(@Query("query") query: String? = null): List<InstrumentDto>

    @POST("api/v1/orders")
    suspend fun createOrder(@Body body: CreateOrderRequestDto): OrderDto

    @GET("api/v1/orders")
    suspend fun orders(@Query("limit") limit: Int = 50, @Query("cursor") cursor: String? = null): OrderListResponseDto

    @GET("api/v1/transactions")
    suspend fun transactions(
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null,
    ): TransactionListResponseDto
}
