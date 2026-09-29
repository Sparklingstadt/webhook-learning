# ExpressでWebhookを学ぶ

Hono版・Go版と同じ注文通知をExpress 5＋TypeScriptで受け取ります。依存パッケージと送信プログラムはプロジェクト直下で共有します。

## 起動

プロジェクト直下（`package.json`があるフォルダ）で実行します。

```sh
npm install
npm run dev:express
```

別のターミナルで同じフォルダから送信します。

```sh
npm run send
```

正常なら送信側に200と `{"received":true,"eventId":"evt_001"}`、受信側に注文IDが表示されます。Hono版・Go版と同じ3000番ポートなので、一度に起動するのは1つにしてください。起動時は`127.0.0.1`だけで待ち受けます。VPNの設定はこの実装には含まれません。

## コードを比較する

受信処理は [app.ts](app.ts) です。

| 処理 | Hono | Express |
| --- | --- | --- |
| アプリ作成 | `new Hono()` | `express()` |
| JSONの読み取り | `await c.req.json()` | `express.json()` → `req.body` |
| 受信口 | `app.post(path, handler)` | `app.post(path, handler)` |
| リクエスト・レスポンス | `c.req`・`c.json()` | `req`・`res` |
| JSON応答 | `c.json(value, 200)` | `res.status(200).json(value)` |
| JSON解析エラー | ルート内の`try/catch` | 4引数のエラーハンドラー |

Expressでは、リクエストが「JSON解析ミドルウェア → ルートの入力チェック → ログ出力 → JSON応答」の順に進みます。解析に失敗した場合はエラーハンドラーへ進みます。`res.json()`はその場で応答を送るため、エラーを返したあとに後続処理へ進まないよう`return`します。

`Content-Type: application/json`を付けて送信してください。正常なイベントは200、壊れたJSONや項目不足は400です。Express版では本文を100KBに制限し、超過時は413を返します。

通知の形式やcurlの練習は[親フォルダの教材](../README.md)と共通です。署名検証・重複排除・永続化は未実装です。将来署名検証を追加するときは、JSON解析前の生の本文を保持する必要があります。

## 検証

```sh
npm run typecheck
npm test
```

Expressのテストは一時的なlocalhostサーバーを起動し、実際のHTTP通信で確認して終了します。

参考：[Express公式API](https://expressjs.com/ja/5x/api/)
