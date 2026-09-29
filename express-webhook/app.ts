import express, { type ErrorRequestHandler } from 'express'

export const app = express()

// ExpressではミドルウェアがJSONを解析し、req.bodyに格納します。
app.use(express.json({ strict: false, limit: '100kb' }))

app.get('/', (_req, res) => {
  res.json({ message: 'Webhook学習サーバー', endpoint: 'POST /webhooks/orders' })
})

app.post('/webhooks/orders', (req, res) => {
  const event: unknown = req.body

  // Hono版と同じ入力チェック。JSONとして読めても、正しいイベントとは限りません。
  if (
    typeof event !== 'object' || event === null ||
    !('id' in event) || typeof event.id !== 'string' || !event.id.trim() ||
    !('type' in event) || event.type !== 'order.created' ||
    !('data' in event) || typeof event.data !== 'object' || event.data === null ||
    !('orderId' in event.data) || typeof event.data.orderId !== 'string' || !event.data.orderId.trim()
  ) {
    res.status(400).json({ error: 'id、type: order.created、data.orderIdを指定してください' })
    return
  }

  console.log(`[受信] event=${event.id} 注文=${event.data.orderId}`)
  res.status(200).json({ received: true, eventId: event.id })
})

// JSON解析エラーはルートに届く前に発生するため、最後に専用の処理を置きます。
// Expressのエラーハンドラーには、使わなくても4つの引数が必要です。
const handleError: ErrorRequestHandler = (error, _req, res, next) => {
  if (error.type === 'entity.parse.failed') {
    res.status(400).json({ error: '本文を正しいJSONで送ってください' })
    return
  }
  if (error.type === 'entity.too.large') {
    res.status(413).json({ error: '本文は100KB以内にしてください' })
    return
  }
  next(error)
}
app.use(handleError)
