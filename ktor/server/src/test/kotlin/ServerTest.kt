package com.example

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerTest {

    @Test
    fun `health live returns 200`() = testApplication {
        application { module() }
        val response = client.get("/health/live")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
