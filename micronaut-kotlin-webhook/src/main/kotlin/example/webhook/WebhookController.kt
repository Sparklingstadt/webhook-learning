package example.webhook

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.json.JsonMapper
import java.io.IOException

// コンストラクターの引数でJSON解析用の部品を受け取ります（依存性注入）。
@Controller("/")
class WebhookController(private val json: JsonMapper) {
    @Get
    fun index() = mapOf("message" to "Webhook学習サーバー", "endpoint" to "POST /webhooks/orders")

    @Post("/webhooks/orders")
    fun receive(@Body body: String): HttpResponse<*> {
        val value = try {
            json.readValue(body, Any::class.java)
        } catch (_: IOException) {
            return HttpResponse.badRequest(mapOf("error" to "本文を正しいJSONで送ってください"))
        }
        // as? は型が違うとnullを返す安全なキャスト、?. はnull時にアクセスを省略します。
        val event = value as? Map<*, *>
        val id = event?.get("id") as? String
        val data = event?.get("data") as? Map<*, *>
        val orderId = data?.get("orderId") as? String
        if (id.isNullOrBlank() || event["type"] != "order.created" || orderId.isNullOrBlank()) {
            return HttpResponse.badRequest(mapOf("error" to "id、type: order.created、data.orderIdを指定してください"))
        }
        println("[受信] event=$id 注文=$orderId")
        return HttpResponse.ok(mapOf("received" to true, "eventId" to id))
    }
}
