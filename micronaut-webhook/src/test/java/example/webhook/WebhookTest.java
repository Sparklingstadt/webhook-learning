package example.webhook;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.json.JsonMapper;
import java.net.http.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class WebhookTest {
    static EmbeddedServer server;
    static HttpClient client;
    static JsonMapper json;

    @BeforeAll
    static void start() {
        server = ApplicationContext.run(EmbeddedServer.class,
                Map.of("micronaut.server.port", -1, "micronaut.server.host", "127.0.0.1"));
        client = HttpClient.newHttpClient();
        json = server.getApplicationContext().getBean(JsonMapper.class);
    }

    @AfterAll
    static void stop() {
        if (client != null) client.close();
        if (server != null) server.close();
    }

    static HttpResponse<String> post(String body) throws Exception {
        return client.send(HttpRequest.newBuilder(server.getURI().resolve("/webhooks/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void acceptsOrder() throws Exception {
        var response = post("""
                {"id":"evt_001","type":"order.created","data":{"orderId":"order_001"}}
                """);
        assertEquals(200, response.statusCode());
        assertEquals(Map.of("received", true, "eventId", "evt_001"), json.readValue(response.body(), Map.class));
    }

    @Test
    void rejectsBrokenJson() throws Exception {
        var response = post("{");
        assertEquals(400, response.statusCode());
        assertEquals(Map.of("error", "本文を正しいJSONで送ってください"), json.readValue(response.body(), Map.class));
    }

    @Test
    void rejectsInvalidEvents() throws Exception {
        for (var body : new String[]{"null", "[]", "{}",
                "{\"id\":123,\"type\":\"order.created\",\"data\":{\"orderId\":\"o\"}}",
                "{\"id\":\" \",\"type\":\"order.created\",\"data\":{\"orderId\":\"o\"}}",
                "{\"id\":\"e\",\"type\":\"unknown\",\"data\":{\"orderId\":\"o\"}}",
                "{\"id\":\"e\",\"type\":\"order.created\",\"data\":{}}"}) {
            var response = post(body);
            assertEquals(400, response.statusCode(), body);
            assertEquals(Map.of("error", "id、type: order.created、data.orderIdを指定してください"), json.readValue(response.body(), Map.class));
        }
    }

    @Test
    void servesIndex() throws Exception {
        var response = client.send(HttpRequest.newBuilder(server.getURI()).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("POST /webhooks/orders", json.readValue(response.body(), Map.class).get("endpoint"));
    }
}
