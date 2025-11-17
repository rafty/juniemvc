# Create Beer Order 要件定義（改訂版）

本ドキュメントは、Beer 注文（BeerOrder）を新規作成し管理するための要件を、機能/非機能・設計原則・API・データモデル・検証・例外・テスト観点に分けて明確化したものです。実装ガイドではなく、実装が満たすべき要件を定義します。プロジェクトの既存構成（package: `guru.springframework.juniemvc`）および Spring Boot ガイドライン（ユーザ提示の 1〜14）に準拠します。

---

## 1. 目的と範囲
- 目的: クライアントが既存の Beer 情報を用いて Beer 注文（BeerOrder）を作成できるようにする。
- 範囲:
  - BeerOrder と BeerOrderLine の永続化モデル定義
  - 注文作成 API（REST）
  - DTO/バリデーション/マッピング
  - トランザクション境界と例外処理
  - 最小限の検索取得（単一注文取得）
- 範囲外:
  - 在庫引当/配送などの業務オーケストレーション（将来拡張）
  - 複雑な検索（ページングは設計原則のみ明示）

---

## 2. 設計原則（本プロジェクトへ適用）
- 依存注入: コンストラクタインジェクションを使用。必須依存は `final`。
- 可視性: Spring コンポーネントは可能な限り package-private（Controller も含む）。
- 設定: `@ConfigurationProperties` による型付き設定を優先（本機能では必要最小限）。
- トランザクション: Service 層でユースケース単位に付与。読み取りは `readOnly=true`。
- OSIV: 無効化する（`spring.jpa.open-in-view=false`）。
- Web/Persistence 分離: Entity を直接返さない。DTO を使用。
- REST: バージョン付き URL、JSON オブジェクトをトップレベル、camelCase を採用。
- 例外: `@RestControllerAdvice` で集中処理。ProblemDetails 形式を推奨。
- ロギング: SLF4J（`System.out.println` 禁止）。
- テスト: Integration Test は Testcontainers、ランダムポート。

---

## 3. ドメインモデル（要件としての項目定義）

ERD 概要:
- BeerOrder 1 — * BeerOrderLine（親子）
- Beer 1 — * BeerOrderLine（参照）

共通ルール:
- 主キー: `id` は Integer、自動採番でよい。
- 監査: `createdDate`, `updateDate` を保持。
- 楽観ロック: `version` を保持。
- 列の取得戦略: 関連は LAZY を基本とする。

3.1 Beer（既存 Entity を前提に必須項目を再確認）
- 必須: `beerName`(<=100), `beerStyle`(<=40), `upc`(<=30, unique), `price`(BigDecimal, scale=2)
- 任意: `quantityOnHand`
- 監査: `createdDate`, `updateDate`

3.2 BeerOrder（新規）
- 項目:
  - `id`(PK), `version`
  - `customerRef`(<=64, 任意)
  - `paymentAmount`(BigDecimal, scale=2, 任意)
  - `status`(Enum: OrderStatus、初期値 NEW)
  - 監査: `createdDate`, `updateDate`
  - `lines`: 子エンティティのコレクション
- 関連:
  - BeerOrder 1 — * BeerOrderLine（所有者は BeerOrderLine 側）。
  - 親側は `cascade = ALL`, `orphanRemoval = true` を要求。
  - 親子整合ヘルパー（add/remove）を提供すること。

3.3 BeerOrderLine（新規）
- 項目:
  - `id`(PK), `version`
  - `orderQuantity`(必須, >0)
  - `quantityAllocated`(>=0, 省略時 0)
  - `status`(Enum: LineStatus、初期値 NEW)
  - 監査: `createdDate`, `updateDate`
- 関連:
  - `beerOrder` 多対一（必須, LAZY）
  - `beer` 多対一（必須, LAZY）

3.4 Enum（新規）
- OrderStatus: NEW, VALIDATION_PENDING, VALIDATED, ALLOCATED, PARTIALLY_ALLOCATED, PICKED_UP, DELIVERED, CANCELED
- LineStatus: NEW, ALLOCATED, BACKORDER, CANCELED
- 要件: DB には文字列で保存（`@Enumerated(STRING)` 相当の要件）。

---

## 4. リポジトリ（要件）
- Spring Data JPA リポジトリを用意すること。
  - `BeerRepository`（既存）
  - `BeerOrderRepository`
  - `BeerOrderLineRepository`
- 可視性は package-private でよい。命名とパッケージは既存に合わせ `guru.springframework.juniemvc.repositories`。

---

## 5. サービス層（ユースケースとトランザクション境界）

5.1 ユースケース: 注文作成
- 入力: Beer の識別子と行情報を含むコマンド（複数行を許容）。
- 出力: 作成された BeerOrder の識別子、または作成結果 DTO。
- 処理要件:
  - すべて同一トランザクションで行う。
  - 各行の Beer が存在しない場合は 4xx エラーへマッピング可能なアプリケーション例外を送出。
  - 親子整合（親に行を追加し、双方向関係を同期）。
  - 初期ステータス: OrderStatus.NEW / LineStatus.NEW。
  - 検証を満たさない場合は作成しない（ロールバック）。

5.2 ユースケース: 注文取得（単一）
- 入力: orderId
- 出力: BeerOrderDTO（行を含む）。
- 処理要件: 読み取り専用トランザクション。必要に応じてフェッチ戦略を最適化（N+1 回避）。

5.3 実装ポリシー
- Constructor Injection（`final` フィールド）。
- ロギングは SLF4J。ビジネス上重要なイベント（作成成功/失敗）を INFO で記録。

---

## 6. Web API（REST）
- ベースパス: `/api/v1/beer-orders`
- メディアタイプ: `application/json; charset=utf-8`
- JSON プロパティ命名: camelCase

6.1 POST /api/v1/beer-orders
- 概要: Beer 注文の新規作成
- リクエスト: BeerOrderCreateRequest
- レスポンス: 201 Created + BeerOrderResponse（Location ヘッダに新規リソース URL）
- エラー: 400（検証エラー）, 404（Beer 未存在）, 409（重複/整合性）, 500（予期しないエラー）

6.2 GET /api/v1/beer-orders/{id}
- 概要: Beer 注文の取得
- レスポンス: 200 OK + BeerOrderResponse
- エラー: 404（未存在）

6.3 将来拡張（非必須）
- GET 検索 + ページング（`page`, `size`, `sort`）

---

## 7. DTO とバリデーション（要求仕様）

7.1 Request DTO: BeerOrderCreateRequest
- フィールド:
  - `customerRef`(任意, <=64)
  - `paymentAmount`(任意, BigDecimal, scale=2, >=0)
  - `lines`(必須, 1件以上)
- 行要素（BeerOrderLineCreateItem）:
  - `beerId`(必須, 正の整数)
  - `orderQuantity`(必須, 正の整数, >0)
- Jakarta Validation:
  - `@NotNull`, `@NotEmpty`, `@Size`, `@Positive`, `@PositiveOrZero`, `@Digits(integer=17, fraction=2)` など

7.2 Response DTO: BeerOrderResponse
- フィールド:
  - `id`, `version`, `customerRef`, `paymentAmount`, `status`, `createdDate`, `updateDate`
  - `lines`: BeerOrderLineResponse[]（`beerId`, `orderQuantity`, `quantityAllocated`, `status`）

7.3 マッピング
- MapStruct などのコンパイル時マッパー推奨。双方向/LAZY に注意し、必要項目のみをマップ。

---

## 8. 例外とエラーレスポンス
- 集中処理: `@RestControllerAdvice` + `@ExceptionHandler`
- フォーマット: RFC 9457 ProblemDetails 準拠（`type`, `title`, `status`, `detail`, `instance`）。
- 主な例外:
  - `BeerNotFoundException` → 404
  - `InvalidOrderException`（検証/整合性エラー）→ 400
  - その他 → 500（詳細はマスク）

---

## 9. セキュリティ/運用
- Actuator: `/health`, `/info`, `/metrics` の最小公開。その他は認証必須。
- 機密情報はログに出力しない。
- i18n: エラーメッセージ等は ResourceBundle 化を前提にキーで管理可能な設計にする。

---

## 10. パフォーマンス/永続化ポリシー
- 取得戦略: 関連は LAZY。必要に応じてフェッチジョインやエンティティグラフを使用。
- N+1 回避: GET 詳細で子行を効率的に取得。
- トランザクションは最小限に短く保つ。

---

## 11. 構成/設定
- `spring.jpa.open-in-view=false` を有効化。
- 必要に応じて Jackson の日付フォーマット ISO-8601（UTC）を使用。

---

## 12. テスト要件
- ユニットテスト:
  - Service のビジネスロジック（検証、例外、親子整合）。
- 統合テスト（Testcontainers, RANDOM_PORT）:
  - POST 正常系: 201, レスポンス body に作成内容反映。
  - POST 異常系: 存在しない beerId で 404。
  - GET 正常系: 200, 行が取得可能。
- リポジトリテスト: エンティティの保存/関連永続化/楽観ロックの基本動作。

---

## 13. 受け入れ基準（Acceptance Criteria）
- API:
  - POST `/api/v1/beer-orders` で 1件以上の行を含む注文が作成できる。
  - 存在しない `beerId` を含む場合、注文は作成されず 404 を返す。
  - GET `/api/v1/beer-orders/{id}` で作成済み注文が取得でき、行情報が含まれる。
- データ整合:
  - 保存後、BeerOrder と BeerOrderLine が正しく関連付く（外部キー制約有効）。
  - 初期ステータスは OrderStatus.NEW / LineStatus.NEW。
- 非機能:
  - OSIV は無効、DTO で返却、ログに機密情報なし、ProblemDetails フォーマットでエラーを返却。
- コード規約:
  - 依存はコンストラクタインジェクション、Spring コンポーネントは package-private を基本とする。

---

## 14. 将来拡張の指針（参考）
- 引当/在庫同期: LineStatus/OrderStatus 遷移ルール追加。
- ページング検索: `GET /api/v1/beer-orders?page=&size=&sort=`
- 顧客概念追加: Customer との関連付け。
- 監査の拡張: 作成者/更新者（セキュリティ連携）。

---

## 付録 A: DDL イメージ（参考・非拘束）
- 参考として、以下のようなカラムを用いる（実際の自動 DDL 生成と乖離可）。

```
beer(id PK, version, beer_name, beer_style, upc UNIQUE, quantity_on_hand, price(19,2), created_date, update_date)
beer_order(id PK, version, customer_ref, payment_amount(19,2), status, created_date, update_date)
beer_order_line(id PK, version, beer_order_id, beer_id, order_quantity, quantity_allocated, status, created_date, update_date)
```

---

この文書に従い、実装は既存プロジェクト構造とガイドラインに整合すること。