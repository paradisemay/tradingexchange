package com.tradingexchange.app.data.remote

import com.tradingexchange.app.domain.model.AppError
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class RemoteErrorMapper(private val json: Json) {
    fun map(throwable: Throwable): AppError {
        if (throwable is IOException) return AppError.Network
        if (throwable is HttpException) {
            val body = throwable.response()?.errorBody()?.string()
            val errorCode = body?.let {
                runCatching { json.decodeFromString(ErrorResponseDto.serializer(), it).errorCode }.getOrNull()
            }
            return when (errorCode) {
                "UNAUTHORIZED" -> AppError.Unauthorized
                "VALIDATION_ERROR" -> AppError.Validation
                "INSUFFICIENT_FUNDS" -> AppError.InsufficientFunds
                "INSUFFICIENT_POSITION" -> AppError.InsufficientPosition
                "QUOTE_UNAVAILABLE" -> AppError.QuoteUnavailable
                "INSTRUMENT_NOT_FOUND" -> AppError.NotFound
                else -> when (throwable.code()) {
                    401 -> AppError.Unauthorized
                    404 -> AppError.NotFound
                    422 -> AppError.Unknown("Операция отклонена")
                    503 -> AppError.QuoteUnavailable
                    else -> AppError.Unknown(throwable.message())
                }
            }
        }
        return AppError.Unknown(throwable.message ?: "Неизвестная ошибка")
    }
}
