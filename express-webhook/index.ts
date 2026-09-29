import { app } from './app.js'

// デフォルトはこのMac自身からの接続だけを受け付けます。
const hostname = process.env.HOST || '127.0.0.1'
app.listen(3000, hostname, () => {
  console.log(`Express学習サーバー: ${hostname}:3000`)
}).on('error', (error) => {
  console.error('起動できませんでした:', error.message)
  process.exitCode = 1
})
