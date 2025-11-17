# BeerOrderShipment 追加実装 計画（Detailed Implementation Plan)

本計画は `prompts/add-order-shipment/requirements.md` の要件に基づき、BeerOrderShipment エンティティの追加と、それに付随する Repository/Service/Controller、DTO/Mapper、Flyway マイグレーション、OpenAPI ドキュメント、各種テストの追加を、最小の影響範囲で実装・検証するための詳細なステップを示します。併せて、提供された Spring Boot ガイドラインに準拠します。

---

## 0. 前提整理とスコープ
- 追加するドメイン: BeerOrderShipment（親: BeerOrder）。
- 関係:
  - BeerOrder 1 : N BeerOrderShipment（BeerOrder が親）
  - BeerOrderShipment は BaseEntity を継承。
- BeerOrderShipment のプロパティ:
  - shipmentDate: not null（必須）
  - carrier: 文字列（nullable 可）
  - trackingNumber: 文字列（nullable 可）
- API ベースパス: `/api/v1/beer-orders/{beerOrderId}` 配下で BeerOrderShipment に対する CRUD を提供。
- DB: 既存プロジェクトの方針に従い Flyway + H2 準拠 SQL。
- ガイドライン準拠:
  - Constructor Injection（依存は final）
  - Spring コンポーネントは可能な限り package-private
  - DTO + MapStruct で Web/Persistence 分離
  - Transaction 境界の明確化（@Transactional）
  - Open Session In View 無効（既存設定の尊重）
  - テストでは Testcontainers 推奨（プロジェクトの状況に合わせて選択）

---

## 1. ドメインと永続化層

1.1 エンティティ追加（src/main/java/.../domain 配下を想定）
- クラス: `BeerOrderShipment extends BaseEntity`
- フィールド:
  - `LocalDate shipmentDate`（@Column(nullable=false)）
  - `String carrier`
  - `String trackingNumber`
  - `BeerOrder beerOrder`（@ManyToOne(fetch = LAZY)、外部キー: beer_order_id、not null）
- 付与アノテーション:
  - `@Entity`, `@Table(name = "beer_order_shipment")`
  - Lombok: `@Getter @Setter` もしくは `@Data`、`@Builder`（継承があるため `@SuperBuilder` を使用）
  - 監査系（BaseEntity の設計に合わせて）
- 双方向関係: BeerOrder 側に `@OneToMany(mappedBy="beerOrder", cascade = CascadeType.ALL, orphanRemoval = true)` のコレクション `shipments` を追加。
  - 影響範囲が大きくなる場合は双方向を避け、必要メソッドで片方向運用も可。既存 BeerOrder の使用箇所に合わせて決定。

1.2 リポジトリ追加
- インターフェース: `BeerOrderShipmentRepository extends JpaRepository<BeerOrderShipment, UUID>`
- 検索メソッド:
  - `Page<BeerOrderShipment> findAllByBeerOrderId(UUID beerOrderId, Pageable pageable)`
  - `Optional<BeerOrderShipment> findByIdAndBeerOrderId(UUID id, UUID beerOrderId)`

1.3 Flyway マイグレーション（H2 準拠 SQL）
- 追加ファイル: `src/main/resources/db/migration/Vx__add_beer_order_shipment.sql`
  - 実際のバージョン番号 x は既存マイグレーションの最新版+1 に設定。
- SQL 内容（2ステップ方針: 列追加 → 外部キー付与）：
  - `CREATE TABLE beer_order_shipment (...);`
  - `ALTER TABLE beer_order_shipment ADD CONSTRAINT fk_beer_order_shipment_beer_order FOREIGN KEY (beer_order_id) REFERENCES beer_order(id);`
- インデックス:
  - `beer_order_id` にインデックス作成。
  - 検索頻度に応じて `shipment_date` 等にもインデックスを検討。

---

## 2. DTO と Mapper（MapStruct）

2.1 DTO 定義（package-private record を推奨）
- `BeerOrderShipmentRequest`（作成/更新用）
  - `LocalDate shipmentDate`（@NotNull）
  - `String carrier`
  - `String trackingNumber`
- `BeerOrderShipmentResponse`（出力用）
  - `UUID id`
  - `LocalDate shipmentDate`
  - `String carrier`
  - `String trackingNumber`

2.2 Mapper
- インターフェース: `BeerOrderShipmentMapper`
  - `@Mapper(componentModel = "spring")`
  - メソッド:
    - `BeerOrderShipment toEntity(BeerOrderShipmentRequest dto)`
    - `BeerOrderShipmentResponse toResponse(BeerOrderShipment entity)`
    - 既存エンティティ更新用: `void update(@MappingTarget BeerOrderShipment target, BeerOrderShipmentRequest source)`
- 注意: beerOrder の紐付けは Service 層で親取得後に設定（Mapper では設定しない）。

---

## 3. サービス層

3.1 インターフェース: `BeerOrderShipmentService`
- メソッド（いずれも `beerOrderId` を第1引数で受ける）:
  - `BeerOrderShipmentResponse create(UUID beerOrderId, BeerOrderShipmentRequest request)`
  - `BeerOrderShipmentResponse getById(UUID beerOrderId, UUID id)`
  - `Page<BeerOrderShipmentResponse> list(UUID beerOrderId, Pageable pageable)`
  - `BeerOrderShipmentResponse update(UUID beerOrderId, UUID id, BeerOrderShipmentRequest request)`
  - `void delete(UUID beerOrderId, UUID id)`

3.2 実装: `BeerOrderShipmentServiceImpl`（package-private, constructor injection）
- 依存:
  - `BeerOrderRepository`（既存）
  - `BeerOrderShipmentRepository`
  - `BeerOrderShipmentMapper`
- トランザクション境界:
  - 参照系: `@Transactional(readOnly = true)`
  - 変更系: `@Transactional`
- 振る舞い:
  - 親 BeerOrder の存在検証（見つからない場合は独自例外 or `EntityNotFoundException`）
  - create: 親を取得 → `mapper.toEntity(request)` → `entity.setBeerOrder(parent)` → 保存 → `mapper.toResponse` 返却
  - update: `findByIdAndBeerOrderId` で対象取得 → `mapper.update(entity, request)` → 保存 → `toResponse`
  - list: `findAllByBeerOrderId(... pageable ...)` → `map(mapper::toResponse)`
  - delete: `findByIdAndBeerOrderId` 取得後に `delete`
- 例外ハンドリング: 既存の `@RestControllerAdvice`（あれば）に統合。なければ ProblemDetails 形式での共通ハンドラを追加検討。

---

## 4. Web 層（REST Controller）

4.1 ベースパスとエンドポイント
- Base: `/api/v1/beer-orders/{beerOrderId}/shipments`
- Endpoints:
  - POST `/` : Create
  - GET `/` : List (pageable)
  - GET `/{id}` : Get by id
  - PUT `/{id}` : Update (全更新)
  - DELETE `/{id}` : Delete

4.2 実装指針
- クラスは package-private。
- Constructor Injection（final フィールド）。
- `@Validated` 使用、DTO の `@NotNull` 等で入力検証。
- 戻り値は `ResponseEntity<...>`。作成時は 201 + Location ヘッダ。
- JSON プロパティは camelCase に統一。

---

## 5. OpenAPI ドキュメント更新（Redocly 構成に準拠）

5.1 paths 追加
- 追加ファイル（file-per-path ルール）:
  - `openapi/openapi/paths/beer-orders_{beerOrderId}_shipments.yaml`（一覧/作成）
  - `openapi/openapi/paths/beer-orders_{beerOrderId}_shipments_{id}.yaml`（単一取得/更新/削除）
- `openapi/openapi/openapi.yaml` の `paths:` に `$ref` 追記。

5.2 components 追加
- `components/schemas` に `BeerOrderShipmentRequest.yaml`, `BeerOrderShipmentResponse.yaml` を追加。
- 必要に応じて `Problem.yaml` 既存再利用。

5.3 記述内容
- セキュリティ（必要なら既存スキームを参照）。
- パラメータ: `beerOrderId`, `id`（UUID）
- ページング: 既存の共通 `parameters` があれば再利用。なければ簡易的に `page`, `size`, `sort` を追加。
- レスポンス: 201/200/204/404/400 など。ヘッダ `Location` を作成時に付与。

5.4 Lint/Build
- `openapi` ディレクトリ配下で `npm ci && npm test`（`redocly lint`）。

---

## 6. テスト戦略

6.1 Mapper ユニットテスト
- `BeerOrderShipmentMapper` の toEntity, toResponse, update を検証。

6.2 Repository テスト
- `@DataJpaTest` でクエリメソッド（`findAllByBeerOrderId`, `findByIdAndBeerOrderId`）を検証。
- 可能であれば Testcontainers（本番DB種別に合わせる）。既存が H2 であれば H2 で簡易実行。

6.3 Service テスト
- 変更系メソッドでの親存在チェック、Mapper 呼び出し、リポジトリ保存の流れを Mock（Mockito）で検証。
- トランザクション境界は Spring コンテキスト統合テストで副作用（フラッシュ/コミット）動作を一部確認。

6.4 Controller テスト
- `@WebMvcTest` でバリデーション・ステータスコード・JSON 形状の検証。
- 統合観点で `@SpringBootTest(webEnvironment = RANDOM_PORT)` による Happy path も 1,2 本追加。

6.5 OpenAPI Lint
- Redocly の lint を CI に統合（最低限ローカルで `npm test`）

---

## 7. 実装順序（推奨）
1) Flyway マイグレーションファイル作成（スキーマ）
2) エンティティ + リポジトリ
3) DTO + Mapper
4) サービス層
5) コントローラ層
6) OpenAPI 追加・修正
7) テスト作成（Mapper → Repository → Service → Controller → 統合）
8) Lint/Build/全テスト実行

---

## 8. 設計・実装上の注意
- コンストラクタインジェクションを徹底（`final` フィールド）。
- 可能な限り package-private 可視性。
- OSIV は無効前提。必要な関連はサービス内で明示的にロード。
- 例外はグローバルハンドラへ（`ProblemDetails` 推奨）。
- MapStruct 変更時は再コンパイルが必要。`mvn -q -DskipTests compile` 等で生成確認。
- JSON のトップレベルはオブジェクト固定。プロパティ名は camelCase。

---

## 9. 受け入れ条件（Acceptance Criteria）
- BeerOrderShipment のテーブルが Flyway により生成され、`beer_order` と外部キーで結合される。
- Repository/Service/Controller による CRUD が `/api/v1/beer-orders/{beerOrderId}/shipments` 配下で動作。
- DTO/Mapper によりエンティティが直接 Web に露出しない。
- すべての新規テストがグリーン。既存テストに影響なし。
- OpenAPI が lint を通過し、エンドポイント・スキーマが反映されている。

---

## 10. リスクと緩和
- 既存 BeerOrder との双方向関連の導入が予期せぬ副作用を生む可能性 → 片方向で開始し、必要に応じて調整。
- バージョン競合（Flyway） → 既存 V の確認後に連番採番、競合時は V を再調整。
- DTO/Mapper の不整合 → Mapper テストと Controller JSON スナップショット的検証で早期検知。

---

## 11. タスク分解（チェックリスト）
- [ ] V?__add_beer_order_shipment.sql 追加
- [ ] BeerOrderShipment エンティティ作成
- [ ] BeerOrderShipmentRepository 作成
- [ ] DTO（Request/Response）作成
- [ ] MapStruct Mapper 作成 + 生成確認
- [ ] Service インターフェース/実装作成
- [ ] REST Controller 作成
- [ ] OpenAPI paths + schemas 追加
- [ ] Mapper テスト
- [ ] Repository テスト
- [ ] Service テスト
- [ ] Controller テスト（WebMvcTest）
- [ ] 統合テスト（RANDOM_PORT）
- [ ] OpenAPI lint 実行（`openapi` ディレクトリ）
- [ ] mvn test 実行で全件グリーン
