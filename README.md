# Webhook Learning

同じ「注文作成」のWebhookを6種類の構成で実装し、HTTPの送受信・JSON解析・入力チェックを比較する日本語の学習用リポジトリです。

| 実装 | 言語 | 教材 |
| --- | --- | --- |
| Hono | TypeScript | このREADME・[受信コード](src/app.ts) |
| Express | TypeScript | [起動手順と比較](express-webhook/README.md) |
| Go標準ライブラリ | Go | [起動手順と比較](go-webhook/README.md) |
| FastAPI | Python | [起動手順と比較](fastapi-webhook/README.md) |
| Micronaut | Java | [起動手順と比較](micronaut-webhook/README.md) |
| Micronaut | Kotlin | [起動手順と比較](micronaut-kotlin-webhook/README.md) |

どの実装も `POST /webhooks/orders` で同じ通知を受信します。通常は `127.0.0.1:3000` で起動するため、実行するのは一度に1つです。署名検証・重複排除・永続化はまだ実装していない、ローカル学習用のサンプルです。

以下はWebhookの基本とHono版の手順です。他の実装は上のリンクを参照してください。

Webhookは、イベントが起きた側から、あらかじめ決めたURLにHTTPリクエストを送って通知する仕組みです。この教材では「注文が作成された」という架空のイベントを使います。実際の注文や決済は発生しません。

通常のAPI利用では、自分から「新しい注文はありますか？」と問い合わせます。Webhookでは、注文サービス側から「注文が作成されました」と通知が届きます。受信側では、普通のHTTPルートとして実装できます。

```text
送信元（scripts/send.ts）                  受信先（Hono）
          ── POST /webhooks/orders + JSON ──▶
                                              本文を確認
                                              注文IDをログ表示
          ◀── HTTP 200 + { received: true } ──
```

今回は両方を同じPCで動かします。Honoは受信口を作るフレームワークで、外部サービスのイベントを自動で発生させるものではありません。

## 1. 動かす

Node.js 24で動作確認する教材です。初回はこのフォルダで依存関係を入れます。

```sh
npm install
npm run dev
```

別のターミナルでもこのフォルダを開き、通知を送ります。

```sh
npm run send
```

送信側にHTTPステータス`200`と`{ received: true, eventId: 'evt_001' }`が表示されます。サーバー側には次のログが出ます。

```text
[受信] event=evt_001 注文=order_001
```

ブラウザで http://127.0.0.1:3000 を開くと案内のJSONが表示されます。ブラウザのアドレス欄で開く操作はGETなので、POSTのWebhook通知とは別です。サーバーの終了はCtrl+Cです。

### LAN内の別端末から接続する

Hono版は通常の起動を停止して `npm run dev:lan`、Go版は `go-webhook` フォルダで `HOST=0.0.0.0 go run ./cmd/server` を実行します。同じポートなのでどちらか一方を起動してください。

別端末のブラウザで `http://<サーバーを起動したMacのLAN IP>:3000` を開きます。`localhost` はアクセスする端末自身を指します。`0.0.0.0` は待受用の指定で、ブラウザの接続先ではありません。LAN IPはMacの「システム設定 → ネットワーク」で接続中のネットワークの詳細から確認できます。

つながらなければ、両端末が同じLANにいること、ゲストWi-Fiの端末間通信制限、VPN、Macのファイアウォールによる受信制限を確認してください。署名検証のない学習用サーバーなので、信頼できるLAN内で使用します。

## 2. 受信コードを読む

まずは `src/app.ts` を読んでください。

| コード | 役割 |
| --- | --- |
| `app.post('/webhooks/orders', ...)` | このURLへのPOSTを受け付ける |
| `await c.req.json()` | 送られた本文をJSONとして読み取る |
| `if (...)` | 外部から来たデータの項目を確認する |
| `console.log(...)` | 通知を受けて処理する場所 |
| `c.json(..., 200)` | 送信元へ受信成功を返す |

`id`は通知のID、`type`はイベントの種類、`data.orderId`は注文のIDです。これらはこの教材で決めた形式であり、すべてのWebhookに共通の規格ではありません。

## 3. 自分でHTTPリクエストを送る

送信スクリプトの代わりにcurlでも試せます。

```sh
curl -i http://127.0.0.1:3000/webhooks/orders \
  -H 'Content-Type: application/json' \
  -d '{"id":"evt_002","type":"order.created","data":{"orderId":"order_002"}}'
```

`-d`を指定するとcurlはPOSTで本文を送ります。`-i`でレスポンスのステータスやヘッダーも表示できます。

次は項目を不足させてみます。

```sh
curl -i http://127.0.0.1:3000/webhooks/orders \
  -H 'Content-Type: application/json' \
  -d '{"type":"order.created"}'
```

期待結果は`400 Bad Request`です。受信ログは出ません。壊れたJSONも400になります。

## 4. 小さな練習

1. `scripts/send.ts`の注文IDを変え、サーバー側のログが変わることを確認する。
2. `npm run send`を2回実行する。同じ通知IDでも2回処理されることを確認する。
3. 受信処理にログを1行追加してみる。

2番目の動作は次のテーマにつながります。実際のWebhookでは送信元が失敗・タイムアウト後に再送する場合があり、同じイベントが複数回届くことを考慮します。再送条件はサービスによって異なります。この送信スクリプトは自動再送しません。

## 5. 次に学ぶこと

この第1段階では、署名検証、重複排除、永続化をまだ実装していません。ローカル専用の教材です。

学習の順番は「受信 → 署名検証 → 重複排除 → 外部サービス連携」がおすすめです。

- **署名検証**：送信元の仕様に従って、共有シークレットと受信した生の本文から署名を検証する。JSONを再生成するとバイト列が変わり得るため、署名に使う本文の扱いを学ぶ。
- **重複排除（冪等性）**：同じイベントIDによる処理を二重に実行しない。実運用ではDBの一意制約などを利用する。
- **応答と再送**：重い処理をキューへ渡す場合も、確実に受け付けたことを確認してから成功を返す。
- **外部サービス連携**：GitHubなどの通知を受ける。外部サービスからこのPCの`127.0.0.1`には届かないため、HTTPSの公開先などが必要になる。

## 動作チェック

```sh
npm run typecheck
npm test
```

テストはHonoの`app.request()`で受信処理を呼びます。HTTPサーバーの起動は不要です。

## 公式資料

- [HonoをNode.jsで動かす](https://hono.dev/docs/getting-started/nodejs)
- [HonoRequest：本文やヘッダーの取得](https://hono.dev/docs/api/request)
- [Honoのテスト](https://hono.dev/docs/guides/testing)
