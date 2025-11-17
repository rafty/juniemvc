# BeerOrderShipment 追加実装 タスクリスト（Detailed Checklist）

以下のタスクは、`prompts/add-order-shipment/plan.md` の詳細計画に基づき、順番に実施できるよう細分化しています。各項目は完了時に [ ] を [x] に更新してください。

---

## 0. 前提確認・準備
1. [x] 既存のエンティティ階層（BaseEntity, BeerOrder）とパッケージ構成を確認する。
2. [x] Spring Boot ガイドライン（コンストラクタインジェクション、package-private、DTO/Mapper、@Transactional、OSIV 無効）を再確認する。
3. [x] 既存の Flyway マイグレーションの最新バージョン番号を確認し、次のバージョンを確定する。
4. [x] OpenAPI の Redocly 設定（openapi/redocly.yaml, openapi/openapi/openapi.yaml）と既存 paths 命名規則を確認する。

---

## 1. スキーマ（Flyway マイグレーション）
5. [x] 新規マイグレーションファイルを作成（例: `src/main/resources/db/migration/V<next>__add_beer_order_shipment.sql`）。
6. [x] `beer_order_shipment` テーブルを作成（id, created_date 等の監査列は BaseEntity に合わせる）。
7. [x] NOT NULL 制約つき `shipment_date` 列を追加。
8. [x] `carrier` 列（文字列）を追加。
9. [x] `tracking_number` 列（文字列）を追加。
10. [x] `beer_order_id` 列（親 FK 用, not null）を追加。
11. [x] 外部キー制約を追加（別ステートメントで実行）：`beer_order_id -> beer_order(id)`。
12. [x] パフォーマンスのためのインデックスを作成（`beer_order_id`、必要に応じて `shipment_date`）。
13. [x] H2 準拠の文法であることを確認。

---

## 2. ドメイン・リポジトリ
14. [x] エンティティ `BeerOrderShipment` を追加（`@Entity`, `@Table(name = "beer_order_shipment")`）。
15. [x] `BaseEntity` を継承し、監査フィールドの互換性を確認。
16. [x] フィールド実装：`LocalDate shipmentDate`（nullable=false）、`String carrier`、`String trackingNumber`。
17. [x] 親関連：`@ManyToOne(fetch = LAZY)` `BeerOrder beerOrder`、`@JoinColumn(name = "beer_order_id", nullable=false)`。
18. [x] Lombok アノテーション付与（`@Getter`/`@Setter` または `@Data`、継承ありの場合は `@SuperBuilder`、`@NoArgsConstructor`, `@AllArgsConstructor`）。
19. [x] 双方向関連が必要か検討し、今回は影響範囲縮小のため非必須と判断。BeerOrder 側への `shipments` 追加は見送り（今後の要件で必要になれば追加）。
20. [x] `BeerOrderShipmentRepository` を追加し、`JpaRepository<BeerOrderShipment, UUID>` を継承。
21. [x] クエリメソッドを定義：`Page<BeerOrderShipment> findAllByBeerOrderId(UUID beerOrderId, Pageable pageable)`。
22. [x] クエリメソッドを定義：`Optional<BeerOrderShipment> findByIdAndBeerOrderId(UUID id, UUID beerOrderId)`。

---

## 3. DTO と Mapper（MapStruct）
23. [x] `BeerOrderShipmentRequest`（record/クラス, package-private）を作成：`@NotNull LocalDate shipmentDate`, `String carrier`, `String trackingNumber`。
24. [x] `BeerOrderShipmentResponse`（record/クラス, package-private）を作成：`UUID id`, `LocalDate shipmentDate`, `String carrier`, `String trackingNumber`。
25. [x] `BeerOrderShipmentMapper` を作成：`@Mapper(componentModel = "spring")`。
26. [x] Mapper メソッド：`BeerOrderShipment toEntity(BeerOrderShipmentRequest dto)`。
27. [x] Mapper メソッド：`BeerOrderShipmentResponse toResponse(BeerOrderShipment entity)`。
28. [x] 既存エンティティ更新メソッド：`void update(@MappingTarget BeerOrderShipment target, BeerOrderShipmentRequest source)`。
29. [x] 親関連（beerOrder）のアサインは Mapper では行わない方針であることを明記し、サービスで設定する。

---

## 4. サービス層
30. [x] `BeerOrderShipmentService` インターフェースを作成。
31. [x] サービスメソッドを定義：`create(UUID beerOrderId, BeerOrderShipmentRequest request)`。
32. [x] サービスメソッドを定義：`getById(UUID beerOrderId, UUID id)`。
33. [x] サービスメソッドを定義：`list(UUID beerOrderId, Pageable pageable)`。
34. [x] サービスメソッドを定義：`update(UUID beerOrderId, UUID id, BeerOrderShipmentRequest request)`。
35. [x] サービスメソッドを定義：`delete(UUID beerOrderId, UUID id)`。
36. [x] `BeerOrderShipmentServiceImpl` を実装（package-private・constructor injection）。
37. [x] 依存注入：`BeerOrderRepository`, `BeerOrderShipmentRepository`, `BeerOrderShipmentMapper`（すべて `final`）。
38. [x] 参照系に `@Transactional(readOnly = true)` を付与。
39. [x] 変更系に `@Transactional` を付与。
40. [x] create 実装：親 BeerOrder 取得→ `mapper.toEntity(request)` → `entity.setBeerOrder(parent)` → 保存 → `mapper.toResponse`。
41. [x] update 実装：`findByIdAndBeerOrderId` で取得 → `mapper.update(entity, request)` → 保存 → `toResponse`。
42. [x] list 実装：`findAllByBeerOrderId(... pageable ...)` → `map(mapper::toResponse)`。
43. [x] delete 実装：`findByIdAndBeerOrderId` で取得 → `repository.delete(entity)`。
44. [x] 親・子が見つからない場合の例外処理（既存の GlobalExceptionHandler/ProblemDetails と統合）。

---

## 5. Web 層（REST Controller）
45. [x] コントローラを追加（package-private, constructor injection, `@Validated`）。
46. [x] Base パスを `/api/v1/beer-orders/{beerOrderId}/shipments` とする。
47. [x] POST `/` 実装：`201 Created` + `Location` ヘッダを返却。
48. [x] GET `/` 実装：ページング対応で一覧返却。
49. [x] GET `/{id}` 実装：単一取得。
50. [x] PUT `/{id}` 実装：全更新。
51. [x] DELETE `/{id}` 実装：`204 No Content`。
52. [x] JSON プロパティ命名を camelCase に統一。
53. [x] `ResponseEntity<T>` で明示的にステータスコードを返す。

---

## 6. OpenAPI ドキュメント（Redocly）
54. [x] paths ファイルを追加：`openapi/openapi/paths/beer-orders_{beerOrderId}_shipments.yaml`（一覧/作成）。
55. [x] paths ファイルを追加：`openapi/openapi/paths/beer-orders_{beerOrderId}_shipments_{id}.yaml`（取得/更新/削除）。
56. [x] `openapi/openapi/openapi.yaml` の `paths:` に `$ref` を追記。
57. [x] components/schemas に `BeerOrderShipmentRequest.yaml` を追加。
58. [x] components/schemas に `BeerOrderShipmentResponse.yaml` を追加。
59. [x] パラメータ定義：`beerOrderId`, `id` は UUID。ページングが既存にあれば再利用、なければ `page`, `size`, `sort` を定義。
60. [x] レスポンスステータスと `Location` ヘッダ（作成時）を定義。
61. [x] Redocly Lint を実行（`cd openapi && npm ci && npm test`）し、エラーを解消。

---

## 7. テスト
62. [x] Mapper ユニットテスト：`toEntity`, `toResponse`, `update` を検証。
63. [x] Repository テスト（`@DataJpaTest`）：`findAllByBeerOrderId`, `findByIdAndBeerOrderId` を検証。
64. [x] Service テスト（Mockito で依存をモック）：親存在チェック、Mapper 呼び出し、保存・削除の動作を検証。
65. [x] Controller テスト（`@WebMvcTest`）：バリデーション、ステータスコード、JSON 形状を検証。
66. [x] 統合テスト（`@SpringBootTest(webEnvironment = RANDOM_PORT)`）：Happy path を 1〜2 本追加。
67. [x] OpenAPI Lint の CI 連携を検討（最低限ローカルで成功すること）。

---

## 8. ビルド・最終確認
68. [x] MapStruct 生成物確認（`mvn -q -DskipTests compile` など）。
69. [x] 単体・統合テストを実行（`mvn -q test`）。
70. [x] Spring Boot アプリの起動確認（必要に応じて）。
71. [x] コードスタイル・可視性（package-private）・コンストラクタインジェクション遵守の再確認。
72. [x] 例外ハンドリングが一貫して ProblemDetails 形式で返ることを確認。
73. [x] 変更点のドキュメント化（README やコミットメッセージ）。

---

補足:
- 変更は最小影響範囲で進め、既存 API/挙動に影響しないよう注意する。
- OSIV 無効を前提に、必要な関連はサービス内で明示取得する。
- OpenAPI の `$ref` は相対パスで、命名規則（`_` 区切り、パラメータは `{}`）に従う。