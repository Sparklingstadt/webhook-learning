package example.webhook;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.json.JsonMapper;
import java.io.IOException;
import java.util.Map;

@Controller("/")
public class WebhookController {
    private final JsonMapper json;

    // MicronautがJSON解析用の部品を渡します（依存性注入）。
    public WebhookController(JsonMapper json) { this.json = json; }

    @Get
    public Map<String, String> index() {
        return Map.of("message", "Webhook学習サーバー", "endpoint", "POST /webhooks/orders");
    }

    @Post("/webhooks/orders")
    public HttpResponse<?> receive(@Body String body) {
        Object value;
        try {
            value = json.readValue(body, Object.class);
        } catch (IOException error) {
            return HttpResponse.badRequest(Map.of("error", "本文を正しいJSONで送ってください"));
        }
        // JSONとして正しくても、項目や値が正しいとは限りません。
        if (!(value instanceof Map<?, ?> event)
                || !(event.get("id") instanceof String id) || id.isBlank()
                || !"order.created".equals(event.get("type"))
                || !(event.get("data") instanceof Map<?, ?> data)
                || !(data.get("orderId") instanceof String orderId) || orderId.isBlank()) {
            return HttpResponse.badRequest(Map.of("error", "id、type: order.created、data.orderIdを指定してください"));
        }
        System.out.printf("[受信] event=%s 注文=%s%n", id, orderId);
        return HttpResponse.ok(Map.of("received", true, "eventId", id));
    }
}
