import { serve } from '@hono/node-server'
import { app } from './app.js'

// LANから接続するときは HOST=0.0.0.0 を指定します。
const hostname = process.env.HOST || '127.0.0.1'
serve({ fetch: app.fetch, hostname, port: 3000 }, () => {
  console.log(`待受アドレス: ${hostname}:3000`)
  console.log('このPC: http://127.0.0.1:3000')
  if (hostname === '0.0.0.0') console.log('別端末: http://<このPCのLAN IP>:3000')
})
