# Create Beer Order タスクリスト（詳細・番号付き）

本タスクリストは `prompts/create-beer-order/plan.md` の実装計画に基づく実行手順です。各項目は完了時に [x] を付けて進捗を管理してください。

[x] 1 フェーズ0: 準備と基盤設定
   [x] 1.1 依存関係確認（MapStruct/Lombok の有効性確認、ビルド）
   [x] 1.2 共通設定: `spring.jpa.open-in-view=false` を `src/main/resources/application.properties` に追加
   [x] 1.3 パッケージ構成方針の確認（entities/repositories/services/controllers/mappers/models/web）

[x] 2 フェーズ1: ドメインモデルの拡張
   [x] 2.1 Enum 追加（package-private）
      - [x] 2.1.1 `OrderStatus { NEW, VALIDATION_PENDING, VALIDATED, ALLOCATED, PARTIALLY_ALLOCATED, PICKED_UP, DELIVERED, CANCELED }`
      - [x] 2.1.2 `LineStatus { NEW, ALLOCATED, BACKORDER, CANCELED }`
   [x] 2.2 Entity: BeerOrder 追加
      - [x] 2.2.1 フィールド定義: `id`, `version`, `customerRef(<=64)`, `paymentAmount(scale=2)`, `status(default=NEW)`, `createdDate`, `updateDate`
      - [x] 2.2.2 リレーション: `@OneToMany(mappedBy="beerOrder", cascade=ALL, orphanRemoval=true, fetch=LAZY) List<BeerOrderLine> lines`
      - [x] 2.2.3 監査: `@CreationTimestamp`/`@UpdateTimestamp`（または Spring Data 監査）
      - [x] 2.2.4 親子整合メソッド: `addLine` / `removeLine`
   [x] 2.3 Entity: BeerOrderLine 追加
      - [x] 2.3.1 フィールド定義: `id`, `version`, `orderQuantity(>0)`, `quantityAllocated(>=0, default 0)`, `status(default=NEW)`, `createdDate`, `updateDate`
      - [x] 2.3.2 関連: `@ManyToOne(fetch=LAZY, optional=false) BeerOrder beerOrder`
      - [x] 2.3.3 関連: `@ManyToOne(fetch=LAZY, optional=false) Beer beer`
      - [x] 2.3.4 監査アノテーションの付与
   [x] 2.4 既存 Beer Entity の整合確認（`upc` unique、`price` scale など）と必要な補強

[x] 3 フェーズ2: リポジトリ追加
   [x] 3.1 `BeerOrderRepository extends JpaRepository<BeerOrder, Integer>` を追加（package-private）
   [x] 3.2 `BeerOrderLineRepository extends JpaRepository<BeerOrderLine, Integer>` を追加（package-private）
   [x] 3.3 可能なら `@EntityGraph(attributePaths = "lines")` の `findByIdWithLines(Integer id)` を追加

[x] 4 フェーズ3: DTO とバリデーション
   [x] 4.1 Request DTO: `BeerOrderCreateRequest`
      - [x] 4.1.1 `String customerRef (<=64, optional)`
      - [x] 4.1.2 `BigDecimal paymentAmount (>=0, scale=2, optional)`（`@Digits(integer=17, fraction=2)`）
      - [x] 4.1.3 `List<BeerOrderLineCreateItem> lines`（`@NotEmpty`）
   [x] 4.2 Request DTO: `BeerOrderLineCreateItem`
      - [x] 4.2.1 `Integer beerId (@NotNull, @Positive)`
      - [x] 4.2.2 `Integer orderQuantity (@NotNull, @Positive)`
   [x] 4.3 Response DTO: `BeerOrderResponse`
      - [x] 4.3.1 `Integer id, Integer version, String customerRef, BigDecimal paymentAmount, OrderStatus status, Instant createdDate, Instant updateDate`
      - [x] 4.3.2 `List<BeerOrderLineResponse> lines`
   [x] 4.4 Response DTO: `BeerOrderLineResponse`
      - [x] 4.4.1 `Integer beerId, Integer orderQuantity, Integer quantityAllocated, LineStatus status`
   [x] 4.5 Controller に `@Validated` 付与、DTO に Jakarta Validation アノテーション付加
   [x] 4.6 JSON 命名は camelCase を維持（Jackson 既定の確認）

[x] 5 フェーズ4: マッピング層
   [x] 5.1 `BeerOrderMapper` 作成
      - [x] 5.1.1 `BeerOrder -> BeerOrderResponse` マッピング
      - [x] 5.1.2 `BeerOrderLine -> BeerOrderLineResponse` マッピング
      - [x] 5.1.3 循環参照回避のため Entity→DTO 片方向のみ定義

[x] 6 フェーズ5: サービス層（トランザクション境界）
   [x] 6.1 例外クラス作成
      - [x] 6.1.1 `BeerNotFoundException extends RuntimeException`
      - [x] 6.1.2 `InvalidOrderException extends RuntimeException`
   [x] 6.2 インタフェース `BeerOrderService` 作成
      - [x] 6.2.1 `BeerOrderResponse create(BeerOrderCreateRequest request)`（`@Transactional`）
      - [x] 6.2.2 `BeerOrderResponse getById(Integer id)`（`@Transactional(readOnly = true)`）
   [x] 6.3 実装 `BeerOrderServiceImpl` 作成（package-private, `@Service`）
      - [x] 6.3.1 依存: `BeerRepository`, `BeerOrderRepository`, `BeerOrderLineRepository`（必要なら）, `BeerOrderMapper`（コンストラクタインジェクション, final）
      - [x] 6.3.2 create 実装: DTO 整合チェック、`BeerOrder` 生成、各 `BeerOrderLine` 生成、親子整合、`save`
      - [x] 6.3.3 getById 実装: `findById` または `findByIdWithLines` で取得、未検出時に例外
      - [x] 6.3.4 ロギング: 成功/失敗時に INFO ログ（機密情報を含めない）

[x] 7 フェーズ6: Web API（Controller/Advice）
   [x] 7.1 `BeerOrderController` 作成（package-private, `@RestController`）
      - [x] 7.1.1 ベースパス `/api/v1/beer-orders`
      - [x] 7.1.2 POST `/` → `201 Created` + `Location: /api/v1/beer-orders/{id}` + `BeerOrderResponse`
      - [x] 7.1.3 GET `/{id}` → `200 OK` + `BeerOrderResponse`
      - [x] 7.1.4 依存はコンストラクタインジェクション、`@Validated` 付与
   [x] 7.2 集中例外処理 `GlobalExceptionHandler` 作成（`@RestControllerAdvice`）
      - [x] 7.2.1 `BeerNotFoundException` → 404 ProblemDetails
      - [x] 7.2.2 `InvalidOrderException` → 400 ProblemDetails
      - [x] 7.2.3 `MethodArgumentNotValidException` → 400 ProblemDetails（Validation エラー詳細を `errors` 拡張に格納）
      - [x] 7.2.4 RFC 9457（Problem Details）形式に準拠

[x] 8 フェーズ7: パフォーマンス/永続化最適化
   [x] 8.1 `findByIdWithLines` に `@EntityGraph(attributePaths = "lines")` を付与し GET で利用
   [x] 8.2 監査フィールドに Hibernate のタイムスタンプを採用（ラウンドトリップ最小化）
   [x] 8.3 `lines` コレクションの初期化・操作方針（`ArrayList` 初期化、add/remove 管理）

[x] 9 フェーズ8: テスト
   [x] 9.1 サービス ユニットテスト
      - [x] 9.1.1 正常系: 全ての `beerId` が存在 → 保存成功、期待レスポンス
      - [x] 9.1.2 異常系: 存在しない `beerId` → `BeerNotFoundException` でロールバック
      - [x] 9.1.3 異常系: 行空/数量<=0 → Validation または `InvalidOrderException`
   [x] 9.2 リポジトリテスト
      - [x] 9.2.1 親子保存（BeerOrder + BeerOrderLine）と version/監査更新の確認
   [x] 9.3 統合テスト（`@SpringBootTest(RANDOM_PORT)` + H2）
      - [x] 9.3.1 POST 正常: 201 / Location / JSON 検証
      - [x] 9.3.2 POST 異常: 存在しない `beerId` → 404
      - [x] 9.3.3 GET 正常: 200, 行含有

[ ] 10 フェーズ9: ロギング/セキュリティ/運用
    [ ] 10.1 ログ出力のガード（機密情報を出さない、`isDebugEnabled` など）
    [ ] 10.2 Actuator 公開制御（`/health`, `/info`, `/metrics` のみ匿名想定）
    [ ] 10.3 i18n メッセージ基盤の準備（`messages.properties` 追加は任意）

[ ] 11 フェーズ10: 実装順序チェックリスト（総合）
    [ ] 11.1 Enum 追加（OrderStatus, LineStatus）
    [ ] 11.2 Entity 追加（BeerOrder, BeerOrderLine）+ 親子整合メソッド
    [ ] 11.3 Repository 追加（BeerOrderRepository, BeerOrderLineRepository）+ EntityGraph
    [ ] 11.4 DTO 追加（CreateRequest/LineItem/Response/LineResponse）+ Validation
    [ ] 11.5 Mapper 追加（BeerOrderMapper）
    [ ] 11.6 Service 追加（BeerOrderService/Impl）+ 例外クラス + ロギング + トランザクション
    [ ] 11.7 Controller 追加（POST, GET）+ Location ヘッダ
    [ ] 11.8 GlobalExceptionHandler 追加（ProblemDetails）
    [ ] 11.9 application.properties に `spring.jpa.open-in-view=false`
    [ ] 11.10 テスト実装（ユニット/リポジトリ/統合）

[ ] 12 フェーズ11: 受け入れ基準トレースと最終確認
    [ ] 12.1 API ステータス/内容の要件充足を確認（Controller/Service/Mapper/DTO）
    [ ] 12.2 データ整合（cascade, orphanRemoval, 双方向整合）をテストで担保
    [ ] 12.3 非機能要件（OSIV 無効、DTO 返却、ログ、ProblemDetails）を確認
    [ ] 12.4 ガイドライン準拠（Constructor Injection, package-private）を全クラスで確認

備考:
- 進捗は各チェックボックスを [x] にして管理してください。
- 実装中に設計変更が入る場合は `plan.md` を更新し、このタスクリストも追随してください。
