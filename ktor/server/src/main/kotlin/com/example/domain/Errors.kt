package com.example.domain

import io.ktor.http.HttpStatusCode

enum class ErrorCode(val httpStatus: HttpStatusCode) {
    VALIDATION_ERROR(HttpStatusCode.BadRequest),
    UNAUTHORIZED(HttpStatusCode.Unauthorized),
    FORBIDDEN(HttpStatusCode.Forbidden),
    INSTRUMENT_NOT_FOUND(HttpStatusCode.NotFound),
    INSUFFICIENT_FUNDS(HttpStatusCode.UnprocessableEntity),
    INSUFFICIENT_POSITION(HttpStatusCode.UnprocessableEntity),
    QUOTE_UNAVAILABLE(HttpStatusCode.ServiceUnavailable),
    CONFLICT(HttpStatusCode.Conflict),
    INTERNAL_ERROR(HttpStatusCode.InternalServerError),
}

class AppException(
    val errorCode: ErrorCode,
    message: String,
    val details: Map<String, String> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(message, cause)
