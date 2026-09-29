package example.webhook

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.json.JsonMapper
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicronautKotlinWebhookTest {
    private lateinit var server: EmbeddedServer
    private lateinit var client: HttpClient
    private lateinit var json: JsonMapper

    @BeforeAll
    fun start() {
        server = ApplicationContext.run(EmbeddedServer::class.java,
            mapOf("micronaut.server.port" to -1, "micronaut.server.host" to "127.0.0.1"))
        client = HttpClient.newHttpClient()
        json = server.applicationContext.getBean(JsonMapper::class.java)
    }
    @AfterAll
    fun stop() {
        if (::client.isInitialized) client.close()
        if (::server.isInitialized) server.close()
    }
    private fun post(body: String): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(server.uri.resolve("/webhooks/orders"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString())

    @Test
    fun acceptsOrder() {
        val response = post("""{"id":"evt_001","type":"order.created","data":{"orderId":"order_001"}}""")
        assertEquals(200, response.statusCode())
        assertEquals(mapOf("received" to true, "eventId" to "evt_001"), json.readValue(response.body(), Map::class.java))
    }
    @Test
    fun rejectsBrokenJson() {
        val response = post("{")
        assertEquals(400, response.statusCode())
        assertEquals(mapOf("error" to "本文を正しいJSONで送ってください"), json.readValue(response.body(), Map::class.java))
    }
    @Test
    fun rejectsInvalidEvents() {
        val bodies = listOf("null", "[]", "{}",
            """{"id":123,"type":"order.created","data":{"orderId":"o"}}""",
            """{"id":" ","type":"order.created","data":{"orderId":"o"}}""",
            """{"id":"e","type":"unknown","data":{"orderId":"o"}}""",
            """{"id":"e","type":"order.created","data":{}}""",
            """{"id":"e","type":"order.created","data":{"orderId":123}}""",
            """{"id":"e","type":"order.created","data":{"orderId":" "}}""")
        for (body in bodies) {
            val response = post(body)
            assertEquals(400, response.statusCode(), body)
            assertEquals(mapOf("error" to "id、type: order.created、data.orderIdを指定してください"), json.readValue(response.body(), Map::class.java))
        }
    }
    @Test
    fun servesIndex() {
        val response = client.send(HttpRequest.newBuilder(server.uri).GET().build(), HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode())
        assertEquals("POST /webhooks/orders", json.readValue(response.body(), Map::class.java)["endpoint"])
    }
}
