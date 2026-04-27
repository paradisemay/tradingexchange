package com.tradingexchange.app

import com.tradingexchange.app.data.remote.RemoteErrorMapper
import com.tradingexchange.app.domain.model.AppError
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {
    private val mapper = RemoteErrorMapper(Json { ignoreUnknownKeys = true })

    @Test
    fun ioExceptionMapsToNetwork() {
        assertEquals(AppError.Network, mapper.map(IOException("offline")))
    }
}
