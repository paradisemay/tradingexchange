package com.example

import com.example.domain.AppException
import com.example.domain.ErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ErrorResponse(
    val errorCode: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val traceId: String = "",
)

fun Application.configureStatusPages() {
    val log = LoggerFactory.getLogger("StatusPages")
    install(StatusPages) {
        exception<AppException> { call, ex ->
            call.respond(ex.errorCode.httpStatus, ErrorResponse(ex.errorCode.name, ex.message ?: "", ex.details))
        }
        exception<Throwable> { call, ex ->
            log.error("Unhandled exception on ${call.request.path()}", ex)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(ErrorCode.INTERNAL_ERROR.name, "An unexpected error occurred"),
            )
        }
    }
}
