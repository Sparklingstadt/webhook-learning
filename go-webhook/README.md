# GoでWebhookを学ぶ

Hono版と同じ注文通知を、Goの標準ライブラリだけで実装しています。追加パッケージのインストールは不要です。Go 1.22以降が必要です。

## 起動と送信

プロジェクトのルートから実行します。Hono版が起動中ならCtrl+Cで停止してください。同じ3000番ポートを使用します。

```sh
cd go-webhook
go run ./cmd/server
```

別のターミナルでも `go-webhook` フォルダを開き、通知を送ります。

```sh
go run ./cmd/send
```

送信側にはHTTPステータス `200` と `{"eventId":"evt_001","received":true}`、受信側には `[受信] event=evt_001 注文=order_001` が表示されます。JSONのキーの並び順は意味に影響しません。

Hono版の `npm run send` や、親フォルダのREADMEのcurlも、そのままGo版へ送信できます。反対にGoの送信プログラムからHono版へ送ることもできます。両者でHTTPの約束を合わせているためです。

## LAN内の別端末から接続する

サーバーを停止して、次のコマンドで起動します。

```sh
HOST=0.0.0.0 go run ./cmd/server
```

別端末から `http://<このMacのLAN IP>:3000` を開きます。別端末での `localhost` はその端末自身です。`0.0.0.0` はすべてのIPv4インターフェースで待ち受ける指定であり、接続先URLにはMacのLAN IPを使います。信頼できるLAN内で使用してください。

## Honoとの比較

受信処理は [internal/webhook/handler.go](internal/webhook/handler.go) です。

| 役割 | Hono / TypeScript | Go |
| --- | --- | --- |
| ルーター作成 | `new Hono()` | `http.NewServeMux()` |
| POSTを受け付ける | `app.post(path, handler)` | `mux.HandleFunc("POST /webhooks/orders", handler)` |
| 本文の取得・JSON解析 | `await c.req.json()` | `io.ReadAll` と `json.Unmarshal` |
| 値の型の確認 | `typeof` | 型アサーション `value.(型)` |
| ログを出す | `console.log` | `log.Printf` |
| JSONを返す | `c.json(value, status)` | この教材の `writeJSON` 関数 |
| サーバー起動 | `serve(...)` | `server.ListenAndServe()` |

Goでは `http.ResponseWriter` が返事を書き込む先、`*http.Request` が届いたリクエストです。`return` は処理を終了し、レスポンスは `writeJSON` 内で書き込みます。

まず送信プログラムの注文IDを変えてログの変化を確認してください。その後、受信処理のログを追加してみましょう。Go版はファイル変更で自動再起動しないため、変更したらサーバーをCtrl+Cで終了し、再度起動します。

正常な注文は200、壊れたJSON・項目不足・型の不一致は400になります。`GET /` は案内JSONを返します。GoのルーターはWebhookのURLへのGETを405で拒否します（Hono版では404）。

## 検証

```sh
go test ./...
go vet ./...
```

`httptest.NewRecorder` を使い、ポートを開かずにHTTPハンドラーをテストします。

Hono版と同じく、ローカル学習用です。署名検証・重複排除・永続化は次の学習段階で追加します。同じ通知を2回送れば2回ログが出ます。
