// 外部サービスの代わりにWebhookを送る練習用プログラムです。
const event = {
  id: 'evt_001',
  type: 'order.created',
  data: { orderId: 'order_001' },
}

try {
  const response = await fetch('http://127.0.0.1:3000/webhooks/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(event),
  })
  console.log('HTTPステータス:', response.status)
  console.log('受信側からの返事:', await response.json())
  if (!response.ok) process.exitCode = 1
} catch {
  console.error('接続できません。別のターミナルで npm run dev を起動してください。')
  process.exitCode = 1
}
