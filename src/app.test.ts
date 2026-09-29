import assert from 'node:assert/strict'
import { test } from 'node:test'
import { app } from './app.js'

const post = (body: string) => app.request('/webhooks/orders', {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body,
})

test('注文イベントを受信して送信元に成功を返す', async () => {
  const response = await post(JSON.stringify({ id: 'evt_001', type: 'order.created', data: { orderId: 'order_001' } }))
  assert.equal(response.status, 200)
  assert.deepEqual(await response.json(), { received: true, eventId: 'evt_001' })
})

test('壊れたJSONは400になる', async () => {
  assert.equal((await post('{')).status, 400)
})

test('必要な項目の不足や不正な値は400になる', async () => {
  for (const value of [null, {}, { id: 'evt_001', type: 'order.created', data: {} }, { id: 'evt_001', type: 'unknown', data: { orderId: 'order_001' } }]) {
    assert.equal((await post(JSON.stringify(value))).status, 400)
  }
})
