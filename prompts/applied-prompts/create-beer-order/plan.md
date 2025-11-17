# Create Beer Order 実装計画（詳細）

本計画は `prompts/create-beer-order/requirements.md` に定義された要件を満たすための、段階的かつ実務的な実装手順を示す。Spring Boot ガイドライン（コンストラクタインジェクション、package-private、DTO/Validation、集中例外処理、OSIV無効化 ほか）を遵守する。

---

## フェーズ0: 準備と基盤設定
- 0-1. 依存関係確認
  - MapStruct が利用可能か確認（既に `BeerMapper` があるため MapStruct は導入済み）。必要であれば DTO 追加に伴う Mapper 更新を行う。
  - Lombok 有効（既存コードで使用済み）。
- 0-2. 共通設定
  - `spring.jpa.open-in-view=false` を `src/main/resources/application.properties` に設定。
  - Jackson の日時は ISO-8601（UTC）既定で問題ない前提。必要に応じて `ObjectMapper` の設定は別フェーズ。
- 0-3. パッケージ構成方針
  - entities: `guru.springframework.juniemvc.entities`
  - repositories: `guru.springframework.juniemvc.repositories`
  - services: `guru.springframework.juniemvc.services` / `...services.impl`
  - web: `guru.springframework.juniemvc.controllers`
  - mappers: `guru.springframework.juniemvc.mappers`
  - models (DTO): `guru.springframework.juniemvc.models`
  - exceptions/advices: `guru.springframework.juniemvc.web`（もしくは `...controllers` 配下に `advice` サブパッケージ）

---

## フェーズ1: ドメインモデルの拡張
- 1-1. Enum 追加（package-private）
  - `OrderStatus { NEW, VALIDATION_PENDING, VALIDATED, ALLOCATED, PARTIALLY_ALLOCATED, PICKED_UP, DELIVERED, CANCELED }`
  - `LineStatus { NEW, ALLOCATED, BACKORDER, CANCELED }`
  - いずれも `@Enumerated(EnumType.STRING)` を前提に保存（Entity 側で指定）。
- 1-2. Entity: BeerOrder
  - フィールド: `id (Integer, @Id @GeneratedValue)`, `version (@Version)`, `customerRef (<=64)`, `paymentAmount (BigDecimal, scale=2)`, `status (OrderStatus, default NEW)`, `createdDate`, `updateDate`。
  - 子リレーション: `@OneToMany(mappedBy="beerOrder", cascade=ALL, orphanRemoval=true, fetch=LAZY)` `List<BeerOrderLine> lines`。
  - 監査: `@CreationTimestamp`, `@UpdateTimestamp`（Hibernate） または `@CreatedDate`, `@LastModifiedDate`（Spring Data JPA 監査）を採用。既存 Beer に合わせる。
  - 親子整合メソッド: `addLine(BeerOrderLine line)`, `removeLine(BeerOrderLine line)` を実装し、双方向セット。
- 1-3. Entity: BeerOrderLine
  - フィールド: `id`, `version`, `orderQuantity (>0)`, `quantityAllocated (>=0, default 0)`, `status (LineStatus, default NEW)`, `createdDate`, `updateDate`。
  - 関連: `@ManyToOne(fetch=LAZY, optional=false) BeerOrder beerOrder`、`@ManyToOne(fetch=LAZY, optional=false) Beer beer`。
  - 監査アノテーションは BeerOrder と同様。
- 1-4. 既存 Beer Entity の整合確認
  - `upc` unique、`price`(scale=2) 等の要件が満たされていることを確認。必要に応じてカラム定義を補強（後方互換に注意）。

---

## フェーズ2: リポジトリ追加
- 2-1. `BeerOrderRepository extends JpaRepository<BeerOrder, Integer>`
- 2-2. `BeerOrderLineRepository extends JpaRepository<BeerOrderLine, Integer>`
- 2-3. 可視性は package-private とし、パッケージは `guru.springframework.juniemvc.repositories`。
- 2-4. GET 詳細の N+1 回避のため、必要に応じて `@EntityGraph(attributePaths="lines")` 付きの `findByIdWithLines(Integer id)` を追加（実装は JPQL または Spring Data の EntityGraph で対応）。

---

## フェーズ3: DTO とバリデーション
- 3-1. Request DTO: `BeerOrderCreateRequest`
  - `String customerRef (<=64, optional)`
  - `BigDecimal paymentAmount (>=0, scale=2, optional)`
  - `List<BeerOrderLineCreateItem> lines`（`@NotEmpty`）
- 3-2. Request DTO: `BeerOrderLineCreateItem`
  - `Integer beerId (@NotNull, @Positive)`
  - `Integer orderQuantity (@NotNull, @Positive)`
- 3-3. Response DTO: `BeerOrderResponse`
  - `Integer id, Integer version, String customerRef, BigDecimal paymentAmount, OrderStatus status, Instant createdDate, Instant updateDate`
  - `List<BeerOrderLineResponse> lines`
- 3-4. Response DTO: `BeerOrderLineResponse`
  - `Integer beerId, Integer orderQuantity, Integer quantityAllocated, LineStatus status`
- 3-5. Jakarta Validation
  - `@Validated` を Controller に付与、DTO フィールドへ適切にアノテーションを付加。
  - `@Digits(integer=17, fraction=2)` により scale を強制（サービスでの丸めも検討）。
- 3-6. JSON 命名は camelCase（Jackson デフォルト）。

---

## フェーズ4: マッピング層
- 4-1. MapStruct マッパー作成/拡張
  - `BeerOrderMapper`: `BeerOrder -> BeerOrderResponse`、`BeerOrderLine -> BeerOrderLineResponse`。
  - Request から Entity への直接マッピングは複合依存（Beer ロード）が必要なため、サービス内で手動組み立てとする。レスポンスは MapStruct で DTO に変換。
  - LAZY 関連へのアクセスはサービス層で初期化済み前提（取得時は fetch join / EntityGraph を使用）。

---

## フェーズ5: サービス層（トランザクション境界）
- 5-1. 例外クラス
  - `BeerNotFoundException extends RuntimeException`
  - `InvalidOrderException extends RuntimeException`
- 5-2. インタフェース `BeerOrderService`
  - `BeerOrderResponse create(BeerOrderCreateRequest request)`（`@Transactional`）
  - `BeerOrderResponse getById(Integer id)`（`@Transactional(readOnly = true)`）
- 5-3. 実装 `BeerOrderServiceImpl`（package-private, `@Service`）
  - 依存: `BeerRepository`, `BeerOrderRepository`, `BeerOrderLineRepository`（必要なら）, `BeerOrderMapper`。
  - create 実装:
    1) 入力検証（DTO アノテーションで基本担保、追加整合チェックはここで）。
    2) `BeerOrder` を生成、status=NEW、必要フィールド設定。
    3) 各 line について `beerId` で `Beer` を DB から取得（なければ `BeerNotFoundException`）。
    4) `BeerOrderLine` を生成し、`orderQuantity` 設定、`quantityAllocated=0`、`status=NEW`、`beer` をセット。
    5) `beerOrder.addLine(line)` で親子整合。
    6) `BeerOrderRepository.save(beerOrder)` でカスケード保存。
    7) 保存結果を `BeerOrderResponse` にマッピングして返却。
  - getById 実装:
    - `BeerOrderRepository` の通常 `findById` または `findByIdWithLines` を使用し、なければ `InvalidOrderException` か 404 相当の例外。

---

## フェーズ6: Web API（Controller）
- 6-1. `BeerOrderController`（package-private, `@RestController`）
  - ベースパス: `/api/v1/beer-orders`
  - POST `/`:
    - 引数: `@Valid @RequestBody BeerOrderCreateRequest`。
    - 戻り: `ResponseEntity<BeerOrderResponse>`、`201 Created`、`Location` ヘッダ `/api/v1/beer-orders/{id}`。
  - GET `/{id}`:
    - 引数: `@PathVariable Integer id`
    - 戻り: `200 OK + BeerOrderResponse`。
  - 依存はコンストラクタインジェクション（`final`）。
- 6-2. 集中例外処理 `GlobalExceptionHandler`（`@RestControllerAdvice`）
  - `@ExceptionHandler(BeerNotFoundException)` → 404 ProblemDetails
  - `@ExceptionHandler(InvalidOrderException)` → 400 ProblemDetails
  - `@ExceptionHandler(MethodArgumentNotValidException)` → 400 ProblemDetails（Validation エラー詳細を `errors` 拡張に格納）
  - フォーマットは RFC 9457 に準拠（`type`, `title`, `status`, `detail`, `instance`）。

---

## フェーズ7: パフォーマンス/永続化の最適化
- 7-1. GET 詳細で N+1 を防ぐため、`findByIdWithLines` に `@EntityGraph(attributePaths="lines")` を付与して使用。
- 7-2. 監査フィールドは DB ラウンドトリップを増やさないよう Hibernate のタイムスタンプを採用。
- 7-3. 可能なら `lines` は `List` 固定長ではなく `ArrayList` 初期化し、add/remove でのみ変更。

---

## フェーズ8: テスト
- 8-1. ユニットテスト（Service）
  - 正常系: 全ての beerId が存在する場合に `BeerOrder` が保存され、レスポンスの内容が期待通り。
  - 異常系: 行に存在しない `beerId` が含まれると `BeerNotFoundException` が送出されロールバックされる。
  - 異常系: 行が空／数量<=0 で `InvalidOrderException`（もしくは Validation）となる。
- 8-2. リポジトリテスト
  - `BeerOrder` に `BeerOrderLine` を追加して save し、親子が保存されること、version/監査が更新されること。
- 8-3. 統合テスト（`@SpringBootTest(RANDOM_PORT)`）
  - 最小構成として H2 を使用して実施（CI の安定性を優先）。将来的に Testcontainers 化を検討。
  - POST 正常: 201, Location ヘッダ, レスポンス JSON 検証。
  - POST 異常: 存在しない beerId → 404。
  - GET 正常: 200, 行が含まれる。

---

## フェーズ9: ロギング/セキュリティ/運用
- 9-1. ログ
  - Service の create 成功時/失敗時に INFO ログ。
  - 入力 DTO の全量ログは避け、識別子と件数などの非機密メタデータのみ。
- 9-2. Actuator
  - `/health`, `/info`, `/metrics` のみ匿名公開、その他は将来のセキュリティ設定で保護。
- 9-3. i18n
  - エラーメッセージキー化の準備（`messages.properties`）。実装は任意。

---

## フェーズ10: 実装順序チェックリスト
1) Enum 追加（OrderStatus, LineStatus）
2) Entity 追加（BeerOrder, BeerOrderLine）+ 親子整合メソッド
3) Repository 追加（BeerOrderRepository, BeerOrderLineRepository）+ EntityGraph
4) DTO 追加（CreateRequest/LineItem/Response/LineResponse）+ Validation
5) Mapper 追加（BeerOrderMapper）
6) Service 追加（BeerOrderService/Impl）+ 例外クラス + ロギング + トランザクション
7) Controller 追加（POST, GET）+ Location ヘッダ
8) GlobalExceptionHandler 追加（ProblemDetails）
9) application.properties に `spring.jpa.open-in-view=false`
10) テスト実装（ユニット/リポジトリ/統合）

---

## フェーズ11: 受け入れ基準とのトレース
- POST/GET API のステータス・内容 → Controller/Service/Mapper/DTO で満たす。
- データ整合 → Entity 設計（cascade, orphanRemoval, 双方向整合）とテストで担保。
- 非機能（OSIV無効、DTO返却、ログ、ProblemDetails）→ 設定/Advice/設計で対応。
- ガイドライン（Constructor Injection, package-private）→ 全クラスで遵守。

---

## 備考とリスク
- 既存スキーマとの差異がある場合、自動 DDL 更新で対応（本番は Flyway/Migration 推奨）。
- MapStruct の循環参照に注意（Entity -> DTO のみ、DTO から Entity への逆変換は行わない）。
- Double ではなく BigDecimal を徹底、丸めモードは `HALF_UP` を基本とする（必要時サービスで正規化）。
