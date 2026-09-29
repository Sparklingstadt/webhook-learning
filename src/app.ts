import { Hono } from 'hono'

export const app = new Hono()

app.get('/', (c) => c.json({ message: 'Webhook学習サーバー', endpoint: 'POST /webhooks/orders' }))

// 送信元が「注文が作成された」と通知するための受信口です。
app.post('/webhooks/orders', async (c) => {
  // HTTPリクエストの本文をJSONとして読み取ります。
  let event: unknown
  try {
    event = await c.req.json()
  } catch {
    return c.json({ error: '本文を正しいJSONで送ってください' }, 400)
  }

  // TypeScriptの型だけでは、外から来た値の正しさを保証できません。
  if (
    typeof event !== 'object' || event === null ||
    !('id' in event) || typeof event.id !== 'string' || !event.id.trim() ||
    !('type' in event) || event.type !== 'order.created' ||
    !('data' in event) || typeof event.data !== 'object' || event.data === null ||
    !('orderId' in event.data) || typeof event.data.orderId !== 'string' || !event.data.orderId.trim()
  ) {
    return c.json({ error: 'id、type: order.created、data.orderIdを指定してください' }, 400)
  }

  // ここが通知を受けて行う処理。第1段階ではログを表示するだけです。
  console.log(`[受信] event=${event.id} 注文=${event.data.orderId}`)

  // 送信元に受信成功を伝えます。これは購入者への返信ではありません。
  return c.json({ received: true, eventId: event.id }, 200)
})
