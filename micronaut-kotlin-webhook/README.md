# Micronaut＋KotlinでWebhookを学ぶ

Java版と同じURL・JSON形式・応答をKotlinで実装しています。Micronaut 4.10.19、Kotlin 2.3.21、JVMターゲット21の構成です。JDK 21以上が必要です。このMacのJava 24に合わせてMicronaut 4系を使用します。

## 起動と送信

プロジェクト直下から実行します。

```sh
cd micronaut-kotlin-webhook
./gradlew run
```

Gradle Wrapperを同梱しているため、GradleやKotlinの別途インストールは不要です。初回はネットワークから依存関係を取得します。`127.0.0.1:3000`で待ち受けるので、他の版は停止してください。終了はCtrl+Cです。

別のターミナルでプロジェクト直下から `npm run send` を実行するか、curlで送信します。

```sh
curl -i http://127.0.0.1:3000/webhooks/orders \
  -H 'Content-Type: application/json' \
  -d '{"id":"evt_001","type":"order.created","data":{"orderId":"order_001"}}'
```

成功時は200と `{"received":true,"eventId":"evt_001"}` が返り、サーバー側には注文IDのログが出ます。JSONのキーの順序は意味に影響しません。壊れたJSONや項目不足は400です。

## Java版と比較する

[WebhookController.kt](src/main/kotlin/example/webhook/WebhookController.kt)を、[Java版](../micronaut-webhook/src/main/java/example/webhook/WebhookController.java)と並べて読めます。

| 意味 | Java版 | Kotlin版 |
| --- | --- | --- |
| メソッド定義 | `public ... receive(...)` | `fun receive(...)` |
| 変更しない変数 | `final` / 再代入しないローカル変数 | `val` |
| 型チェック・取り出し | `instanceof String id` | `as? String` |
| nullを考慮したアクセス | 明示的に確認 | `?.` |
| null・空白チェック | 型確認後の`isBlank()` | `isNullOrBlank()` |
| Map作成 | `Map.of(...)` | `mapOf("key" to value)` |
| 文字列への埋め込み | `printf` | `"event=$id"` |

`@Controller`・`@Post`・`@Body`・`HttpResponse`はJava版と同じMicronautの仕組みです。`class WebhookController(private val json: JsonMapper)`はコンストラクターの引数とプロパティをまとめて宣言し、MicronautからJSON解析用の部品を受け取ります。

公式のKotlin雛形はGradle構成のため、Java版のMavenとはビルドコマンドが異なります。`build.gradle.kts`はKotlinで書くビルド設定、`gradle.properties`はMicronautなどのバージョン設定です。KSPがコンパイル時にアノテーションを処理します。

署名検証・重複排除・永続化はまだ実装していません。localhost限定の学習用で、VPN設定は含みません。同じ通知を2回送れば2回ログが出ます。

## テスト

```sh
./gradlew test
```

一時的なlocalhostの空きポートでサーバーを起動し、正常通知・不正なJSON・不正なイベント・案内ページをHTTPで確認します。テスト終了時にサーバーも停止します。

参考：[Micronaut Launch](https://micronaut.io/launch/)、[GradleのJava対応表](https://docs.gradle.org/current/userguide/compatibility.html)
