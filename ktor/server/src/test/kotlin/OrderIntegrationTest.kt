package com.example

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderIntegrationTest : BaseIntegrationTest() {


    // Регистрирует пользователя и возвращает accessToken
    private suspend fun ApplicationTestBuilder.registerAndLogin(
        client: io.ktor.client.HttpClient,
        email: String = uniqueEmail(),
        password: String = "secret123"
    ): String {
        val resp = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password","fullName":"Trader"}""")
        }
        return Json.parseToJsonElement(resp.bodyAsText()).jsonObject.str("accessToken")
    }

    // Создаёт MARKET заявку, возвращает тело ответа
    private suspend fun ApplicationTestBuilder.placeOrder(
        client: io.ktor.client.HttpClient,
        token: String,
        ticker: String,
        side: String,
        quantity: String
    ): Pair<HttpStatusCode, JsonObject> {
        val resp = client.post("/api/v1/orders") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"ticker":"$ticker","side":"$side","orderType":"MARKET","quantity":"$quantity"}""")
        }
        return resp.status to Json.parseToJsonElement(resp.bodyAsText()).jsonObject
    }

    private suspend fun ApplicationTestBuilder.getPortfolio(
        client: io.ktor.client.HttpClient,
        token: String
    ): JsonObject {
        val resp = client.get("/api/v1/portfolio") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return Json.parseToJsonElement(resp.bodyAsText()).jsonObject
    }

    // ─── КРИТИЧЕСКИЙ ПУТЬ: buy → portfolio → sell ────────────────────────────

    @Test
    fun `покупка SBER успешно списывает баланс`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        // Начальный баланс 100_000 RUB
        val before = getPortfolio(client, token)
        val cashBefore = BigDecimal(before["cash"]!!.jsonObject.str("available"))

        // Покупаем 10 акций SBER по 252 = 2520 RUB
        val (status, body) = placeOrder(client, token, "SBER", "BUY", "10")

        assertEquals(HttpStatusCode.Created, status)
        assertEquals("FILLED", body.str("status"))
        assertEquals("SBER", body.str("ticker"))

        // Баланс уменьшился
        val after = getPortfolio(client, token)
        val cashAfter = BigDecimal(after["cash"]!!.jsonObject.str("available"))
        assertTrue(cashAfter < cashBefore, "Баланс должен был уменьшиться")

        // В портфеле появилась позиция SBER
        val positions = after["positions"]!!.jsonArray
        val sberPos = positions.firstOrNull {
            it.jsonObject.str("ticker") == "SBER"
        }
        assertTrue(sberPos != null, "В портфеле должна быть позиция SBER")
        assertTrue(BigDecimal(sberPos!!.jsonObject.str("quantity")).compareTo(BigDecimal("10")) == 0, "Количество должно быть 10")
    }

    @Test
    fun `полный путь buy → portfolio → sell восстанавливает баланс`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        val before = getPortfolio(client, token)
        val cashBefore = BigDecimal(before["cash"]!!.jsonObject.str("available"))

        // BUY 5 YNDX по 4100 = 20_500 RUB
        val (buyStatus, _) = placeOrder(client, token, "YNDX", "BUY", "5")
        assertEquals(HttpStatusCode.Created, buyStatus)

        // Проверяем позицию
        val midPortfolio = getPortfolio(client, token)
        val cashMid = BigDecimal(midPortfolio["cash"]!!.jsonObject.str("available"))
        assertTrue(cashMid < cashBefore)

        // SELL 5 YNDX
        val (sellStatus, sellBody) = placeOrder(client, token, "YNDX", "SELL", "5")
        assertEquals(HttpStatusCode.Created, sellStatus)
        assertEquals("FILLED", sellBody.str("status"))

        // Баланс вернулся
        val after = getPortfolio(client, token)
        val cashAfter = BigDecimal(after["cash"]!!.jsonObject.str("available"))
        assertTrue(cashAfter > cashMid, "После продажи баланс должен вырасти")

        // Позиция исчезла или стала 0
        val sberPositions = after["positions"]!!.jsonArray.filter {
            it.jsonObject.str("ticker") == "YNDX"
        }
        assertTrue(
            sberPositions.isEmpty() || sberPositions.first().jsonObject.str("quantity") == "0",
            "Позиция YNDX должна исчезнуть после полной продажи"
        )
    }

    @Test
    fun `история транзакций содержит BUY и SELL операции`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        placeOrder(client, token, "GAZP", "BUY", "5")
        placeOrder(client, token, "GAZP", "SELL", "5")

        val resp = client.get("/api/v1/transactions") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        val txs = body["transactions"]!!.jsonArray
        assertTrue(txs.size >= 2, "Должно быть минимум 2 транзакции")

        val types = txs.map { it.jsonObject.str("type") }.toSet()
        assertTrue("BUY" in types)
        assertTrue("SELL" in types)
    }

    // ─── ГРАНИЧНЫЕ СЛУЧАИ ────────────────────────────────────────────────────

    @Test
    fun `INSUFFICIENT_FUNDS при нехватке денег на покупку`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        // 1000 акций LKOH по 7200 = 7_200_000 RUB >> 100_000 начального баланса
        val (status, body) = placeOrder(client, token, "LKOH", "BUY", "1000")

        assertEquals(HttpStatusCode.UnprocessableEntity, status)
        assertEquals("INSUFFICIENT_FUNDS", body.str("errorCode"))
    }

    @Test
    fun `INSUFFICIENT_POSITION при продаже акций которых нет`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        // Пытаемся продать GMKN — позиции нет
        val (status, body) = placeOrder(client, token, "GMKN", "SELL", "1")

        assertEquals(HttpStatusCode.UnprocessableEntity, status)
        assertEquals("INSUFFICIENT_POSITION", body.str("errorCode"))
    }

    @Test
    fun `INSUFFICIENT_POSITION при продаже больше чем есть в портфеле`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        // Купили 5, пытаемся продать 100
        placeOrder(client, token, "SBER", "BUY", "5")
        val (status, body) = placeOrder(client, token, "SBER", "SELL", "100")

        assertEquals(HttpStatusCode.UnprocessableEntity, status)
        assertEquals("INSUFFICIENT_POSITION", body.str("errorCode"))
    }

    @Test
    fun `баланс не уходит в минус после покупки`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        // Максимально возможная покупка (почти весь баланс)
        placeOrder(client, token, "SBER", "BUY", "390") // 390 * 252 = 98_280

        val portfolio = getPortfolio(client, token)
        val cash = BigDecimal(portfolio["cash"]!!.jsonObject.str("available"))
        assertTrue(cash >= BigDecimal.ZERO, "Баланс не должен быть отрицательным")
    }

    @Test
    fun `покупка несуществующего инструмента возвращает 404`() = testApplication {
        application { testModule(buildConfig()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        val (status, body) = placeOrder(client, token, "FAKE", "BUY", "1")
        assertEquals(HttpStatusCode.NotFound, status)
        assertEquals("INSTRUMENT_NOT_FOUND", body.str("errorCode"))
    }
}
