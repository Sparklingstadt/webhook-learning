import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { once } from 'node:events'
import type { Server } from 'node:http'
import type { AddressInfo } from 'node:net'
import { app } from './app.js'

let server: Server
let baseURL: string
before(async () => {
  server = app.listen(0, '127.0.0.1')
  await once(server, 'listening')
  baseURL = `http://127.0.0.1:${(server.address() as AddressInfo).port}`
})
after(async () => {
  if (server?.listening) await new Promise<void>((resolve, reject) => {
    server.close((error) => error ? reject(error) : resolve())
  })
})

const post = (body: string) => fetch(`${baseURL}/webhooks/orders`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' }, body,
})

test('注文イベントをHTTPで受信する', async () => {
  const response = await post(JSON.stringify({ id: 'evt_001', type: 'order.created', data: { orderId: 'order_001' } }))
  assert.equal(response.status, 200)
  assert.deepEqual(await response.json(), { received: true, eventId: 'evt_001' })
})

test('壊れたJSONに400とJSONエラーを返す', async () => {
  const response = await post('{')
  assert.equal(response.status, 400)
  assert.deepEqual(await response.json(), { error: '本文を正しいJSONで送ってください' })
})

test('項目不足・不正な型・未知の種類は400になる', async () => {
  for (const value of [null, [], {}, { id: 123, type: 'order.created', data: { orderId: 'order_001' } }, { id: 'evt_001', type: 'unknown', data: { orderId: 'order_001' } }, { id: 'evt_001', type: 'order.created', data: {} }]) {
    const response = await post(JSON.stringify(value))
    assert.equal(response.status, 400)
    assert.deepEqual(await response.json(), { error: 'id、type: order.created、data.orderIdを指定してください' })
  }
})

test('100KBを超える本文は413になる', async () => {
  const response = await post(JSON.stringify({ padding: 'x'.repeat(102401) }))
  assert.equal(response.status, 413)
  assert.deepEqual(await response.json(), { error: '本文は100KB以内にしてください' })
})

test('ルートの案内を表示し、WebhookへのGETは404になる', async () => {
  const response = await fetch(baseURL)
  assert.equal(response.status, 200)
  assert.equal((await response.json()).endpoint, 'POST /webhooks/orders')
  const getResponse = await fetch(`${baseURL}/webhooks/orders`)
  assert.equal(getResponse.status, 404)
  await getResponse.text()
})
