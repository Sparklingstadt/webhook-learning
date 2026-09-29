# FastAPIでWebhookを学ぶ

Python＋FastAPIで、Hono・Go・Express版と同じ注文通知を受信します。Python 3.14で動作確認しています。

## 準備・起動

プロジェクト直下から、専用の仮想環境を作ります。

```sh
cd fastapi-webhook
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m uvicorn main:app --host 127.0.0.1 --port 3000 --reload
```

3000番ポートを使うため、他の版は停止してください。localhost限定の起動です。VPNや外部公開の設定は含みません。終了はCtrl+Cです。

別のターミナルで `fastapi-webhook` フォルダを開いて送信します。

```sh
.venv/bin/python send.py
```

送信側に200と `{"received":true,"eventId":"evt_001"}`、受信側に注文IDが表示されます。プロジェクト直下の `npm run send` や既存のcurlもそのまま使えます。

## FastAPIならではの書き方

受信コードは [main.py](main.py) です。

| 役割 | Hono / Express | FastAPI |
| --- | --- | --- |
| 受信口 | `app.post(...)` | `@app.post(...)` |
| JSON解析 | `c.req.json()` / `express.json()` | 引数の型指定で自動実行 |
| 入力チェック | `if`で確認 | Pydanticモデルの`OrderEvent`で宣言 |
| 応答 | `c.json()` / `res.json()` | Pythonの辞書を`return` |
| 操作できるAPIドキュメント | 別途用意 | `/docs`で自動生成 |

`StrictStr`は数値などを文字列として受け入れない指定、`Literal["order.created"]`は種類の限定です。空白だけのIDはバリデーターで拒否します。

FastAPIは通常、入力エラーに422を返します。この教材では例外ハンドラーで400に揃えています。壊れたJSONも400、WebhookへのGETは405です。署名検証・重複排除・永続化はまだ実装していません。

## ブラウザから通知を送る

起動後、http://127.0.0.1:3000/docs を開きます。`POST /webhooks/orders` → `Try it out`で次のJSONを入れ、`Execute`を押してください。

```json
{"id":"evt_002","type":"order.created","data":{"orderId":"order_002"}}
```

`Response body`とサーバー側のログを確認しましょう。次にIDを数字へ変えると400になります。同じイベントを2回送ると2回処理される点は、他の版と同じです。

## 検証

```sh
.venv/bin/python -m pytest -q
```

TestClientで、ネットワークのポートを開かずに受信処理をテストします。`requirements.in`は直接使うパッケージ一覧、`requirements.txt`は検証した依存関係の固定版です。

公式資料：[リクエスト本文](https://fastapi.tiangolo.com/tutorial/body/)・[テスト](https://fastapi.tiangolo.com/tutorial/testing/)
