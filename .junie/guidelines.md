# Spring Boot ガイドライン

## 1. **Field/Setter Injection より Constructor Injection を優先する**

* すべての必須依存関係を `final` フィールドとして宣言し、コンストラクタ経由で注入する。
* コンストラクタが1つしかない場合、`@Autowired` は不要（Spring が自動検出する）。
* 本番コードでは Field Injection や Setter Injection を避ける。

**説明:**

* 必須依存を `final` フィールドにしてコンストラクタで注入することで、オブジェクトはJava言語の仕組みだけで常に初期化済みの正しい状態になる。フレームワーク固有の初期化に依存しない。
* Reflection を使った初期化やモックなしでユニットテストが書ける。
* コンストラクタを見るだけで、そのクラスの依存関係が一目でわかる。
* Spring Boot の `RestClient.Builder`, `ChatClient.Builder` のような拡張ポイントもコンストラクタインジェクションで柔軟に初期化できる。

```java
@Service
public class OrderService {
   private final OrderRepository orderRepository;
   private final RestClient restClient;

   public OrderService(OrderRepository orderRepository, 
                       RestClient.Builder builder) {
       this.orderRepository = orderRepository;
       this.restClient = builder
               .baseUrl("http://catalog-service.com")
               .requestInterceptor(new ClientCredentialTokenInterceptor())
               .build();
   }

   //... methods
}
```

---

## 2. **Spring Component は可能な限り package-private にする**

* Controller、`@Configuration` クラス、`@Bean` メソッドなどは、必要がない限り `public` にせず package-private（デフォルト可視性）にする。

**説明:**

* package-private にすることで、実装の詳細を他のパッケージから隠し、カプセル化を強化できる。
* Spring Boot は package-private なクラスも検出・呼び出し可能なので、外部公開が不要なクラスは `public` にしなくてよい。

---

## 3. **Typed Properties を使って設定を整理する**

* `application.properties` または `.yml` に共通プレフィックスを持つ設定をまとめる。
* `@ConfigurationProperties` クラスにバインドし、バリデーションアノテーションを付与して不正設定を即検知する。
* 環境ごとの設定は Profiles よりも環境変数の利用を推奨。

**説明:**

* 設定キーとバリデーションを1つの `@ConfigurationProperties` Bean に集約することで、メンテナンスが容易になる。
* 各所で `@Value("${...}")` を使うと、設定キー変更のたびに複数箇所を修正する必要がある。
* Profiles を多用すると、複数プロファイルの組み合わせ順序により意図しない設定になる場合がある。

---

## 4. **明確なトランザクション境界を定義する**

* Service 層メソッドごとに1つのトランザクション単位を定義する。
* 読み取り専用メソッドには `@Transactional(readOnly = true)`。
* データ変更を伴うメソッドには `@Transactional`。
* トランザクション内の処理は最小限に保つ。

**説明:**

* **Unit of Work:** Use case単位でDB操作を1つの原子的な処理にまとめる。
* **接続再利用:** `@Transactional` メソッドでは同一接続を使い続け、接続プールのオーバーヘッドを削減。
* **パフォーマンス:** `readOnly = true` で不要なフラッシュを防ぎ、高速化。
* **競合軽減:** トランザクションを短く保つことでロック競合を減らす。

---

## 5. **Open Session in View パターンを無効化する**

* Spring Data JPA 使用時は、`spring.jpa.open-in-view=false` を設定する。

**説明:**

* OSIVを有効にすると View レンダリング時に Lazy ロードが走り、N+1問題を引き起こす。
* 無効にすることで、必要な関連を fetch join や entity graph で明示的に取得し、`LazyInitializationException` を回避できる。

---

## 6. **Web層とPersistence層を分離する**

* Entity を Controller のレスポンスとして直接返さない。
* Request/Response 専用の DTO (record クラス) を定義する。
* 入力検証は `Jakarta Validation` アノテーションで行う。

**説明:**

* Entity を直接公開すると、DBスキーマ変更がAPI仕様に影響する。
* DTOを用いることで公開フィールドを明確に制御でき、安全性・可読性が向上。
* Use-case ごとにDTOを設けると、柔軟なバリデーション設定が可能。
* MapStruct などのコンパイル時マッピングライブラリを使用し、Reflectionによるオーバーヘッドを回避。

---

## 7. **REST API Design Principles に従う**

* `/api/v{version}/resources` のようにバージョン付きでリソース指向のURLを構成する。
* コレクション・サブリソースのURL命名を一貫させる（例: `/posts/{slug}/comments`）。
* `ResponseEntity<T>` で明示的なHTTPステータスを返す。
* 大量データは Pagination を使用する。
* JSONは常にオブジェクト形式をトップレベルにし、拡張性を確保。
* JSONプロパティ名は snake_case または camelCase に統一。

**説明:**

* 一貫性のあるREST設計により、クライアントが予測しやすく、ドキュメントに頼らずに利用できる。
* 標準化されたURL構造とHTTPレスポンスで、信頼性の高いクライアント連携が可能。
* 詳細は [Zalando RESTful API and Event Guidelines](https://opensource.zalando.com/restful-api-guidelines/) を参照。

---

## 8. **ビジネス操作には Command Object を使う**

* 入力データをラップする専用の Command record（例: `CreateOrderCommand`）を定義し、Service メソッドに渡す。

**説明:**

* Use caseごとにCommandやQueryオブジェクトを作成することで、必要な入力項目が明確になる。
* 呼び出し元がどの値を指定すべきか（キーや作成日を自動生成するかなど）を迷うことがなくなる。

---

## 9. **例外処理を集中化する**

* `@ControllerAdvice` または `@RestControllerAdvice` クラスでグローバルハンドラを定義し、`@ExceptionHandler` メソッドで例外を処理する。
* エラーレスポンスは一貫したフォーマットにし、`ProblemDetails`（[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)）形式を推奨。

**説明:**

* 例外はキャッチして標準エラーレスポンスを返すべきで、スロー放置は避ける。
* 各Controllerでtry/catchを書くより、`GlobalExceptionHandler` に集約した方がメンテナンス性が高い。

---

## 10. **Actuator の公開制御**

* `/health`, `/info`, `/metrics` など最低限のエンドポイントのみ認証なしで公開。
* その他は認証必須にする。

**説明:**

* これらのエンドポイントは監視ツール（Prometheusなど）からのヘルスチェックに必要。
* 非本番環境（DEV, QA）では `/actuator/beans`, `/actuator/loggers` などを追加で有効化してもよい。

---

## 11. **ResourceBundle を使った国際化 (i18n)**

* ラベルやメッセージなどユーザー向け文言はコードに直接書かず、ResourceBundleに外部化する。

**説明:**

* 文字列をコードに埋め込むと多言語対応が困難になる。
* ResourceBundleを使えば、ロケールごとに別ファイルで翻訳を管理できる。
* Springがユーザーロケールに応じて適切なBundleをロード可能。

---

## 12. **Integration Test では Testcontainers を使う**

* 実際のサービス（DB, メッセージブローカなど）をDockerコンテナで起動してテストする。

**説明:**

* 本番と同じ種類の依存関係でテストすることで、環境差異を減らし信頼性を高める。
* `latest` タグではなく、使用中の本番依存と同じバージョンのDockerイメージを指定する。

---

## 13. **Integration Test ではランダムポートを使用する**

* 以下のように `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` を指定。

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

**説明:**

* CI/CD 環境では複数ビルドが同時実行されるため、固定ポートよりランダムポートを使用して競合を防ぐ。

---

## 14. **Logging**

* `System.out.println()` は使用禁止。必ず SLF4J（Logback, Log4j2 など）を利用する。
* 機密情報をログに出力しない。
* 高コストなログメッセージ生成はガード節または Supplier/Lambda を使う。

```java
if (logger.isDebugEnabled()) {
    logger.debug("Detailed state: {}", computeExpensiveDetails());
}

// Supplier/Lambda
logger.atDebug()
	.setMessage("Detailed state: {}")
	.addArgument(() -> computeExpensiveDetails())
    .log();
```

**説明:**

* **柔軟なログ制御:** 環境ごとにログレベルを切り替え可能。
* **リッチなメタデータ:** クラス名、スレッドIDなどを自動出力できる。
* **多様な出力:** コンソール、ファイル、DB、リモート転送などに対応。
* **分析しやすい構造化ログ:** ELKやLokiへの連携も容易。

---

## 15. **Flyway によるDBマイグレーション**

* Spring Boot は `spring-boot-starter-data-jpa` や `spring-boot-starter-jdbc` と併用すると、アプリ起動時に Flyway を自動実行する（`spring.flyway.enabled=true` が既定、依存を追加すれば有効）。
* デフォルトのスクリプト配置場所（クラスパス）
  - `db/migration`（推奨の共通ディレクトリ）
  - ベンダー別に分ける場合: `db/migration/{vendor}`（例: `db/migration/postgresql`、`db/migration/mysql`）
  - 一般的なプロジェクト構成例:
    - 本番用: `src/main/resources/db/migration`
    - テスト用: `src/test/resources/db/migration`
* バージョン命名規則（Versioned Migrations）
  - ファイル名: `V<version>__<description>.sql`
  - `<version>` は数値を `.` または `_` で区切る（例: `1`, `1.1`, `2_0_3`）
  - 例: `V1__init_schema.sql`, `V1_1__add_beer_table.sql`, `V2__add_indexes.sql`
* 繰り返しマイグレーション（Repeatable Migrations）
  - ファイル名: `R__<description>.sql`
  - 内容が変わるたびに再実行される（チェックサムで判定）
  - 例: `R__refresh_views.sql`, `R__seed_reference_data.sql`
* Java ベースのマイグレーション
  - クラスパス上のパッケージ `db.migration`（デフォルト）に `V1__*.java` などを配置可能
  - 例: `package db.migration; public class V3__BackfillData implements JavaMigration { ... }`
* よく使う設定（`application.properties`）
  - `spring.flyway.locations=classpath:db/migration`（複数指定可）
  - `spring.flyway.baseline-on-migrate=true`（既存DBへ導入時のベースライン）
  - `spring.flyway.clean-disabled=true`（安全のため本番は必ず無効化）
  - `spring.flyway.schemas=public`（必要に応じてスキーマ指定）
* 運用上の注意
  - マイグレーションは一方向（不可逆）を基本とし、後戻りを想定しない
  - 1コミット=1マイグレーションを心がけ、スキーマ変更とアプリ変更の整合性を保つ
  - 長時間ロックが発生するDDLはメンテ時間帯や段階適用（オンラインDDL対応）を検討する

* データベースの移行には、H2 準拠の SQL 構文を使用します。
* テーブルを変更して外部キー制約を持つプロパティを追加する場合は、まず新しい列を追加し、次に 2 番目の SQL ステートメントで外部キー制約を追加します。 

# Spring Boot ガイドライン

## 1. **Field/Setter Injection より Constructor Injection を優先する**

* すべての必須依存関係を `final` フィールドとして宣言し、コンストラクタ経由で注入する。
* コンストラクタが1つしかない場合、`@Autowired` は不要（Spring が自動検出する）。
* 本番コードでは Field Injection や Setter Injection を避ける。

**説明:**

* 必須依存を `final` フィールドにしてコンストラクタで注入することで、オブジェクトはJava言語の仕組みだけで常に初期化済みの正しい状態になる。フレームワーク固有の初期化に依存しない。
* Reflection を使った初期化やモックなしでユニットテストが書ける。
* コンストラクタを見るだけで、そのクラスの依存関係が一目でわかる。
* Spring Boot の `RestClient.Builder`, `ChatClient.Builder` のような拡張ポイントもコンストラクタインジェクションで柔軟に初期化できる。

```java
@Service
public class OrderService {
   private final OrderRepository orderRepository;
   private final RestClient restClient;

   public OrderService(OrderRepository orderRepository, 
                       RestClient.Builder builder) {
       this.orderRepository = orderRepository;
       this.restClient = builder
               .baseUrl("http://catalog-service.com")
               .requestInterceptor(new ClientCredentialTokenInterceptor())
               .build();
   }

   //... methods
}
```

---

## 2. **Spring Component は可能な限り package-private にする**

* Controller、`@Configuration` クラス、`@Bean` メソッドなどは、必要がない限り `public` にせず package-private（デフォルト可視性）にする。

**説明:**

* package-private にすることで、実装の詳細を他のパッケージから隠し、カプセル化を強化できる。
* Spring Boot は package-private なクラスも検出・呼び出し可能なので、外部公開が不要なクラスは `public` にしなくてよい。

---

## 3. **Typed Properties を使って設定を整理する**

* `application.properties` または `.yml` に共通プレフィックスを持つ設定をまとめる。
* `@ConfigurationProperties` クラスにバインドし、バリデーションアノテーションを付与して不正設定を即検知する。
* 環境ごとの設定は Profiles よりも環境変数の利用を推奨。

**説明:**

* 設定キーとバリデーションを1つの `@ConfigurationProperties` Bean に集約することで、メンテナンスが容易になる。
* 各所で `@Value("${...}")` を使うと、設定キー変更のたびに複数箇所を修正する必要がある。
* Profiles を多用すると、複数プロファイルの組み合わせ順序により意図しない設定になる場合がある。

---

## 4. **明確なトランザクション境界を定義する**

* Service 層メソッドごとに1つのトランザクション単位を定義する。
* 読み取り専用メソッドには `@Transactional(readOnly = true)`。
* データ変更を伴うメソッドには `@Transactional`。
* トランザクション内の処理は最小限に保つ。

**説明:**

* **Unit of Work:** Use case単位でDB操作を1つの原子的な処理にまとめる。
* **接続再利用:** `@Transactional` メソッドでは同一接続を使い続け、接続プールのオーバーヘッドを削減。
* **パフォーマンス:** `readOnly = true` で不要なフラッシュを防ぎ、高速化。
* **競合軽減:** トランザクションを短く保つことでロック競合を減らす。

---

## 5. **Open Session in View パターンを無効化する**

* Spring Data JPA 使用時は、`spring.jpa.open-in-view=false` を設定する。

**説明:**

* OSIVを有効にすると View レンダリング時に Lazy ロードが走り、N+1問題を引き起こす。
* 無効にすることで、必要な関連を fetch join や entity graph で明示的に取得し、`LazyInitializationException` を回避できる。

---

## 6. **Web層とPersistence層を分離する**

* Entity を Controller のレスポンスとして直接返さない。
* Request/Response 専用の DTO (record クラス) を定義する。
* 入力検証は `Jakarta Validation` アノテーションで行う。

**説明:**

* Entity を直接公開すると、DBスキーマ変更がAPI仕様に影響する。
* DTOを用いることで公開フィールドを明確に制御でき、安全性・可読性が向上。
* Use-case ごとにDTOを設けると、柔軟なバリデーション設定が可能。
* MapStruct などのコンパイル時マッピングライブラリを使用し、Reflectionによるオーバーヘッドを回避。

---

## 7. **REST API Design Principles に従う**

* `/api/v{version}/resources` のようにバージョン付きでリソース指向のURLを構成する。
* コレクション・サブリソースのURL命名を一貫させる（例: `/posts/{slug}/comments`）。
* `ResponseEntity<T>` で明示的なHTTPステータスを返す。
* 大量データは Pagination を使用する。
* JSONは常にオブジェクト形式をトップレベルにし、拡張性を確保。
* JSONプロパティ名は snake_case または camelCase に統一。

---

## 8. **ビジネス操作には Command Object を使う**

* 入力データをラップする専用の Command record（例: `CreateOrderCommand`）を定義し、Service メソッドに渡す。

**説明:**

* Use caseごとにCommandやQueryオブジェクトを作成することで、必要な入力項目が明確になる。
* 呼び出し元がどの値を指定すべきか（キーや作成日を自動生成するかなど）を迷うことがなくなる。

---

## 9. **例外処理を集中化する**

* `@ControllerAdvice` または `@RestControllerAdvice` クラスでグローバルハンドラを定義し、`@ExceptionHandler` メソッドで例外を処理する。
* エラーレスポンスは一貫したフォーマットにし、`ProblemDetails`（[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)）形式を推奨。

**説明:**

* 例外はキャッチして標準エラーレスポンスを返すべきで、スロー放置は避ける。
* 各Controllerでtry/catchを書くより、`GlobalExceptionHandler` に集約した方がメンテナンス性が高い。

---

## 10. **Actuator の公開制御**

* `/health`, `/info`, `/metrics` など最低限のエンドポイントのみ認証なしで公開。
* その他は認証必須にする。

**説明:**

* これらのエンドポイントは監視ツール（Prometheusなど）からのヘルスチェックに必要。
* 非本番環境（DEV, QA）では `/actuator/beans`, `/actuator/loggers` などを追加で有効化してもよい。

---

## 11. **ResourceBundle を使った国際化 (i18n)**

* ラベルやメッセージなどユーザー向け文言はコードに直接書かず、ResourceBundleに外部化する。

**説明:**

* 文字列をコードに埋め込むと多言語対応が困難になる。
* ResourceBundleを使えば、ロケールごとに別ファイルで翻訳を管理できる。
* Springがユーザーロケールに応じて適切なBundleをロード可能。

---

## 12. **Integration Test では Testcontainers を使う**

* 実際のサービス（DB, メッセージブローカなど）をDockerコンテナで起動してテストする。

**説明:**

* 本番と同じ種類の依存関係でテストすることで、環境差異を減らし信頼性を高める。
* `latest` タグではなく、使用中の本番依存と同じバージョンのDockerイメージを指定する。

---

## 13. **Integration Test ではランダムポートを使用する**

* 以下のように `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` を指定。

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

**説明:**

* CI/CD 環境では複数ビルドが同時実行されるため、固定ポートよりランダムポートを使用して競合を防ぐ。

---

## 14. **Logging**

* `System.out.println()` は使用禁止。必ず SLF4J（Logback, Log4j2 など）を利用する。
* 機密情報をログに出力しない。
* 高コストなログメッセージ生成はガード節または Supplier/Lambda を使う。

```java
if (logger.isDebugEnabled()) {
    logger.debug("Detailed state: {}", computeExpensiveDetails());
}

// Supplier/Lambda
logger.atDebug()
	.setMessage("Detailed state: {}")
	.addArgument(() -> computeExpensiveDetails())
    .log();
```

**説明:**

* **柔軟なログ制御:** 環境ごとにログレベルを切り替え可能。
* **リッチなメタデータ:** クラス名、スレッドIDなどを自動出力できる。
* **多様な出力:** コンソール、ファイル、DB、リモート転送などに対応。
* **分析しやすい構造化ログ:** ELKやLokiへの連携も容易。

---

## 15. **Flyway によるDBマイグレーション**

* Spring Boot は `spring-boot-starter-data-jpa` や `spring-boot-starter-jdbc` と併用すると、アプリ起動時に Flyway を自動実行する（`spring.flyway.enabled=true` が既定、依存を追加すれば有効）。
* デフォルトのスクリプト配置場所（クラ スパス）
  - `db/migration`（推奨の共通ディレクトリ）
  - ベンダー別に分ける場合: `db/migration/{vendor}`（例: `db/migration/postgresql`、`db/migration/mysql`）
  - 一般的なプロジェクト構成例:
    - 本番用: `src/main/resources/db/migration`
    - テスト用: `src/test/resources/db/migration`
* バージョン命名規則（Versioned Migrations）
  - ファイル名: `V<version>__<description>.sql`
  - `<version>` は数値を `.` または `_` で区切る（例: `1`, `1.1`, `2_0_3`）
  - 例: `V1__init_schema.sql`, `V1_1__add_beer_table.sql`, `V2__add_indexes.sql`
* 繰り返しマイグレーション（Repeatable Migrations）
  - ファイル名: `R__<description>.sql`
  - 内容が変わるたびに再実行される（チェックサムで判定）
  - 例: `R__refresh_views.sql`, `R__seed_reference_data.sql`
* Java ベースのマイグレーション
  - クラスパス上のパッケージ `db.migration`（デフォルト）に `V1__*.java` などを配置可能
  - 例: `package db.migration; public class V3__BackfillData implements JavaMigration { ... }`
* よく使う設定（`application.properties`）
  - `spring.flyway.locations=classpath:db/migration`（複数指定可）
  - `spring.flyway.baseline-on-migrate=true`（既存DBへ導入時のベースライン）
  - `spring.flyway.clean-disabled=true`（安全のため本番は必ず無効化）
  - `spring.flyway.schemas=public`（必要に応じてスキーマ指定）
* 運用上の注意
  - マイグレーションは一方向（不可逆）を基本とし、後戻りを想定しない
  - 1コミット=1マイグレーションを心がけ、スキーマ変更とアプリ変更の整合性を保つ
  - 長時間ロックが発生するDDLはメンテ時間帯や段階適用（オンラインDDL対応）を検討する

## 16. Project Lombokの使用
- Lombokを使用して、定型的なボイラープレートコードを削減します。
- IDEでアノテーション処理（annotation processing）を有効化し、自動的にボイラープレートコードが生成されるようにします。
- クラスにビルダー（builder）を追加する際、そのクラスが別のクラスを継承している場合は、@SuperBuilder を使用してビルダーを定義してください。

## 17. 型変換のためのMapStruct使用
- ドメインオブジェクトとDTOの相互変換には、MapStructを使用します。
- 2つのクラス間のマッピングを設定するために @Mapper を使用します。
- それぞれのフィールド間のマッピングを設定するために @Mapping を使用します。
- マッパー（Mapper）を修正した場合は、プロジェクトを再コンパイルして新しいMapper実装を生成してください。
  既存のエンティティを更新する場合にも、Mapperを使用して更新を行います。

## 18. サービス操作（Service Operations）
- 既存のエンティティを更新する際は、Mapperを使用してエンティティを更新します。
- まず、エンティティをデータベースから取得（fetch）し、その後、Mapperを使ってプロパティを更新してから、
- 更新後のエンティティをデータベースへ保存してください。

---

# OpenAPI ドキュメント運用ガイド（本リポジトリ）

このプロジェクトには、Redocly を用いた OpenAPI 仕様が含まれます。エントリポイントは `openapi/openapi/openapi.yaml` で、`$ref` によるファイル分割・再利用を前提としています。ここでは、ファイル参照の使い方、パス操作とファイル名の対応規則、コンポーネントの定義方法、そして仕様のテスト手順をまとめます。

## 1. OpenAPI 仕様ファイルの場所と構成

- エントリポイント: `openapi/openapi/openapi.yaml`
- 主要フォルダ:
  - `openapi/openapi/paths`: パス（エンドポイント）定義
  - `openapi/openapi/components`: 再利用コンポーネント（schemas, headers, responses など）
  - `openapi/redocly.yaml`: Redocly CLI 設定（`root: openapi/openapi.yaml` を指す）

openapi.yaml から `$ref` で相対パス参照します。例:

```yaml
paths:
  '/users/{username}':
    $ref: 'paths/users_{username}.yaml'
  '/user':
    $ref: 'paths/user.yaml'
  '/user/list':
    $ref: 'paths/user-status.yaml'
  '/echo':
    $ref: 'paths/echo.yaml'
```

## 2. パス操作とファイル名の命名規則

- 基本方針: 「1パス=1ファイル」（file-per-path）。`paths` 直下に配置します。
- 区切り文字: パス区切り `/` はアンダースコア `_` に置き換えます。
- パスパラメータ: `{username}` のようなパラメータはそのまま中括弧付きでファイル名に含めます。
- 例:
  - API パス `/users/{username}` → ファイル `paths/users_{username}.yaml`
  - API パス `/user` → ファイル `paths/user.yaml`
  - API パス `/user/list` → ファイル `paths/user-status.yaml`（このリポジトリでは一部にハイフン `-` を使った例もあります。新規作成時は基本的に `_` を推奨しますが、既存命名に合わせてください）
  - API パス `/pathItem` → ファイル `paths/pathItem.yaml`
  - API パス `/pathItemWithExamples` → ファイル `paths/pathItemWithExamples.yaml`

参考: `openapi/openapi/paths/README.md` に、ファイル-per-path とファイル-per-operation、サブフォルダ運用の選択肢が説明されています。本プロジェクトでは上記の「file-per-path + `_` 区切り」を基本とします。

## 3. コンポーネント（schemas/headers/parameters/responses 等）の定義と参照

- 配置場所: `openapi/openapi/components/<kind>`
  - 例: `components/schemas`, `components/headers`, `components/responses`
- 命名: 各ファイルがコンポーネント名になります（例: `Schema.yaml`, `User.yaml`, `Problem.yaml`, `ExpiresAfter.yaml`）。
- 参照方法: パスファイルや他のコンポーネントから `$ref` で相対参照します。

例1: パス定義からスキーマ・ヘッダ・レスポンスを参照

```yaml
# openapi/openapi/paths/pathItem.yaml（抜粋）
requestBody:
  content:
    application/json:
      schema:
        $ref: ../components/schemas/Schema.yaml
responses:
  '200':
    headers:
      X-Expires-After:
        $ref: ../components/headers/ExpiresAfter.yaml
    content:
      application/json:
        schema:
          $ref: ../components/schemas/Schema.yaml
  '400':
    $ref: ../components/responses/Problem.yaml
```

例2: スキーマ内で他スキーマを参照（discriminator や allOf を含む）

```yaml
# openapi/openapi/components/schemas/User.yaml（抜粋）
discriminator:
  propertyName: userType
  mapping:
    admin: './Admin.yaml'
    basic: './Basic.yaml'
properties:
  email:
    $ref: './Email.yaml'
  exampleObject:
    allOf:
      - $ref: './ExampleObject.yaml'
```

例3: ルート仕様からコンポーネント参照（Webhook の例）

```yaml
# openapi/openapi/openapi.yaml（抜粋）
webhooks:
  userInfo:
    post:
      requestBody:
        content:
          application/json:
            schema:
              $ref: 'components/schemas/User.yaml'
```

## 4. セキュリティスキームの定義

- `components.securitySchemes` にて定義します。例は `openapi.yaml` を参照（`oauth2`、`apiKey`、`http basic` 等）。
- 参照は `security` セクションで行います。

```yaml
security:
  - api_key: []
  - basic_auth: []
```

## 5. OpenAPI 仕様のテスト（lint）手順

このリポジトリは Redocly CLI を使用して lint（検証）します。`openapi/package.json` の `test` スクリプトは `redocly lint` を実行します。

- 事前準備（初回または依存更新時）
  1) プロジェクトルートから `openapi` ディレクトリへ移動:
     - `cd openapi`
  2) 依存インストール:
     - `npm ci`（lockfile を尊重）または `npm install`

- テスト実行
  - `npm test`
    - 実体は `redocly lint` を実行し、`redocly.yaml` の `root: openapi/openapi.yaml` を対象に検証します。

- 補足
  - ドキュメントのプレビュー: `npm start`（`redocly preview-docs`）
  - バンドル生成: `npm run build`（`dist/bundle.yaml` を出力）

## 6. 運用上の注意

- `$ref` は常に相対パスで、参照元ファイルから見た相対位置を記述してください。
- 新規のパスファイル追加時は、`openapi.yaml` の `paths:` に `$ref` を忘れず追記してください。
- 命名規則は、既存のファイルに合わせることを優先しつつ、原則として `_` 区切り＋パラメータは `{}` を維持します。
- コンポーネントは用途別フォルダ（schemas/headers/parameters/responses/...）に分け、ファイル名＝コンポーネント名とします。
