package com.example

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertTrue

class ConcurrentOrderTest : BaseIntegrationTest() {


    @Test
    fun `параллельные покупки не уводят баланс в минус`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }

        // Регистрация
        val regResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"${uniqueEmail()}","password":"password1","fullName":"Trader"}""")
        }
        val token = Json.parseToJsonElement(regResp.bodyAsText()).jsonObject.str("accessToken")

        // Запускаем 20 параллельных покупок по 10 акций SBER = 2520 RUB каждая
        // Итого 20 * 2520 = 50_400 RUB, но заявки исполняются атомарно
        val statuses = coroutineScope {
            (1..20).map {
                async(Dispatchers.IO) {
                    client.post("/api/v1/orders") {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $token")
                        setBody("""{"ticker":"SBER","side":"BUY","orderType":"MARKET","quantity":"10"}""")
                    }.status
                }
            }.awaitAll()
        }

        // Нет 500 ошибок — система не упала
        val serverErrors = statuses.count { it == HttpStatusCode.InternalServerError }
        assertTrue(serverErrors == 0, "Не должно быть 500 ошибок, получено: $serverErrors")

        // Часть прошла (201), часть отклонена (4xx) — всё нормально
        val successful = statuses.count { it == HttpStatusCode.Created }
        assertTrue(successful >= 1, "Хотя бы одна заявка должна пройти")

        // Проверяем что баланс не отрицательный
        val portfolio = client.get("/api/v1/portfolio") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body = Json.parseToJsonElement(portfolio.bodyAsText()).jsonObject
        val cash = BigDecimal(body["cash"]!!.jsonObject.str("available"))
        assertTrue(cash >= BigDecimal.ZERO, "Баланс не должен быть отрицательным! Получено: $cash")
    }

    @Test
    fun `параллельные продажи не уводят позицию в минус`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val regResp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"${uniqueEmail()}","password":"password1","fullName":"Trader"}""")
        }
        val token = Json.parseToJsonElement(regResp.bodyAsText()).jsonObject.str("accessToken")

        // Покупаем 50 акций
        client.post("/api/v1/orders") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"ticker":"GAZP","side":"BUY","orderType":"MARKET","quantity":"50"}""")
        }

        // Запускаем 20 параллельных продаж по 10 акций (итого пытаемся продать 200, есть только 50)
        val statuses = coroutineScope {
            (1..20).map {
                async(Dispatchers.IO) {
                    client.post("/api/v1/orders") {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $token")
                        setBody("""{"ticker":"GAZP","side":"SELL","orderType":"MARKET","quantity":"10"}""")
                    }.status
                }
            }.awaitAll()
        }

        // Нет 500 ошибок
        val serverErrors = statuses.count { it == HttpStatusCode.InternalServerError }
        assertTrue(serverErrors == 0, "Не должно быть 500 ошибок")

        // Проверяем что позиция не ушла в минус
        val portfolio = client.get("/api/v1/portfolio") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val body = Json.parseToJsonElement(portfolio.bodyAsText()).jsonObject
        val positions = body["positions"]!!.jsonArray
        val gazpPos = positions.firstOrNull { it.jsonObject.str("ticker") == "GAZP" }
        if (gazpPos != null) {
            val qty = BigDecimal(gazpPos.jsonObject.str("quantity"))
            assertTrue(qty >= BigDecimal.ZERO, "Количество бумаг не может быть отрицательным! Получено: $qty")
        }
        // Если позиции нет — всё продано, это тоже корректно
    }
}
