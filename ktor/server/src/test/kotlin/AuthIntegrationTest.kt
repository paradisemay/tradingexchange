package com.example

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthIntegrationTest : BaseIntegrationTest() {


    private fun app(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(this)
    }

    @Test
    fun `register returns 201 с токенами`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val resp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"${uniqueEmail()}","password":"secret123","fullName":"Test User"}""")
        }

        assertEquals(HttpStatusCode.Created, resp.status)
        val body = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        assertNotNull(body["userId"])
        assertNotNull(body["accessToken"])
        assertNotNull(body["refreshToken"])
        assertTrue(body.str("accessToken").isNotBlank())
    }

    @Test
    fun `register дважды одним email возвращает 409 CONFLICT`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val email = uniqueEmail()

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"password1","fullName":"User"}""")
        }

        val resp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"password1","fullName":"User"}""")
        }

        assertEquals(HttpStatusCode.Conflict, resp.status)
        val body = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        assertEquals("CONFLICT", body.str("errorCode"))
    }

    @Test
    fun `login с правильными данными возвращает 200 с токенами`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val email = uniqueEmail()

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"mypassword","fullName":"User"}""")
        }

        val resp = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"mypassword"}""")
        }

        assertEquals(HttpStatusCode.OK, resp.status)
        val body = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        assertTrue(body.str("accessToken").isNotBlank())
        assertTrue(body.str("refreshToken").isNotBlank())
    }

    @Test
    fun `login с неверным паролем возвращает 401`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val email = uniqueEmail()

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"correct","fullName":"User"}""")
        }

        val resp = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, resp.status)
        val body = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        assertEquals("UNAUTHORIZED", body.str("errorCode"))
    }

    @Test
    fun `login несуществующего пользователя возвращает 401`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val resp = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"nobody@example.com","password":"password1"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun `refresh выдаёт новую пару токенов`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val email = uniqueEmail()

        val regResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"password1","fullName":"User"}""")
        }
        val regBody = Json.parseToJsonElement(regResp.bodyAsText()).jsonObject
        val oldRefresh = regBody.str("refreshToken")

        val resp = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$oldRefresh"}""")
        }

        assertEquals(HttpStatusCode.OK, resp.status)
        val body = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        assertTrue(body.str("accessToken").isNotBlank())
        // Старый refresh должен быть инвалидирован — rotation
        val resp2 = client.post("/api/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$oldRefresh"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, resp2.status)
    }

    @Test
    fun `запрос портфеля без токена возвращает 401`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val resp = client.get("/api/v1/portfolio")
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun `GET me возвращает профиль пользователя`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val email = uniqueEmail()

        val regResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"password1","fullName":"Иван Иванов"}""")
        }
        val token = Json.parseToJsonElement(regResp.bodyAsText()).jsonObject.str("accessToken")

        val resp = client.get("/api/v1/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, resp.status)
        val body = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        assertEquals(email, body.str("email"))
        assertEquals("Иван Иванов", body.str("fullName"))
        assertEquals("CLIENT", body.str("role"))
    }
}
