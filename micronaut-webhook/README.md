# MicronautでWebhookを学ぶ

Java＋Micronautで、他の版と同じ注文通知を受信します。公式Micronaut LaunchのMaven雛形を使用しています。Maven Wrapperを同梱しているので、Mavenの事前インストールは不要です。

この教材はMicronaut 4.10.19、JDK 21以上を対象にしています。Java 24の既存環境に合わせて4系を使用します。Micronaut 5系はJava 25が必要です（[公式説明](https://micronaut.io/2026/04/27/micronaut-framework-5-0-with-java-25-baseline/)）。

## 起動

プロジェクト直下から実行します。

```sh
cd micronaut-webhook
./mvnw mn:run
```

初回はMavenと依存パッケージをダウンロードします。他の版と同じ3000番ポートを使うため、一度に起動するのは1つです。待受先は `127.0.0.1` に限定しています。終了はCtrl+Cです。

別のターミナルでプロジェクト直下から `npm run send` を使うか、次のcurlで通知を送ります。

```sh
curl -i http://127.0.0.1:3000/webhooks/orders \
  -H 'Content-Type: application/json' \
  -d '{"id":"evt_001","type":"order.created","data":{"orderId":"order_001"}}'
```

正常なら200と `{"received":true,"eventId":"evt_001"}`、受信側には注文IDのログが表示されます。JSONのキー順は異なることがあります。壊れたJSONや項目不足は400です。

## コードを比較する

受信処理は [WebhookController.java](src/main/java/example/webhook/WebhookController.java) です。

| 役割 | Hono / Express | Micronaut |
| --- | --- | --- |
| 受信口 | `app.post(...)` | `@Controller` と `@Post` |
| リクエスト本文 | `c.req` / `req.body` | `@Body`付きの引数 |
| JSON解析 | `c.req.json()` / ミドルウェア | `JsonMapper.readValue()` |
| 成功の応答 | `c.json()` / `res.json()` | `HttpResponse.ok()` |
| 入力エラー | ステータス400を指定 | `HttpResponse.badRequest()` |

`@Controller`や`@Post`はJavaのアノテーションです。MicronautはこれらからHTTPルートを構成します。コンストラクターで要求した `JsonMapper` も自動で渡します。これが依存性注入（DI）の小さな例です。

Micronautでも型付き入力クラスと検証アノテーションを使えます。この教材では他の版との比較を優先し、JSONを読み取って明示的に項目をチェックしています。

署名検証・重複排除・永続化は未実装です。VPNの設定は含みません。同じ通知を2回送れば2回処理されます。

## 検証

```sh
./mvnw test
```

一時的なlocalhostサーバーを空いているポートで起動し、JavaのHTTPクライアントで正常な通知・不正なJSON・項目不足・案内ページを検証して終了します。

公式資料：[MicronautのJava/Maven入門](https://guides.micronaut.io/creating-your-first-micronaut-app-maven-java/)
