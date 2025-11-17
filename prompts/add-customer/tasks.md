# Customer 機能追加 — 作業タスクリスト（チェックリスト）

注意: すべてのタスクは [ ] を [x] に更新して進捗を記録してください。

## [x] 1. 全体指針の確認と初期設定
- [x] 1.1 Spring ガイドライン（コンストラクタ注入・package-private 優先）の遵守方針を確認（既存コードに準拠）
- [x] 1.2 Web 層と永続化層の分離（DTO 使用）方針を確認（DTO/Mapper 方式を採用済み）
- [x] 1.3 トランザクション境界の方針（参照系 readOnly）を確認（サービス層で適用予定）
- [x] 1.4 OSIV 無効化設定（spring.jpa.open-in-view=false）を確認/追加（既に設定済み）
- [x] 1.5 例外処理の集中化（ProblemDetails 互換）方針を確認（GlobalExceptionHandler あり）
- [x] 1.6 SLF4J ロギングの徹底（println 排除）方針を確認（println 使用なし）

## [x] 2. ドメインモデルの準備
- [x] 2.1 BaseEntity の存在確認（id, version, createdDate, updatedDate）
- [x] 2.2 BaseEntity が無ければ @MappedSuperclass で追加（Jakarta Persistence アノテーション）
- [x] 2.3 Customer エンティティの追加（table: customer）
  - [x] 2.3.1 フィールド: name(not null), email, phoneNumber, addressLine1(not null), addressLine2, city(not null), state(not null), postalCode(not null) を定義
  - [x] 2.3.2 Lombok アノテーション（Getter/Setter/Builder/NoArgsConstructor/AllArgsConstructor）を既存スタイルに合わせて付与
  - [x] 2.3.3 BeerOrder との OneToMany（Customer 側）定義（mappedBy="customer"）
- [x] 2.4 BeerOrder エンティティに ManyToOne Customer を追加（LAZY, @JoinColumn("customer_id"), nullable で後方互換）
- [x] 2.5 equals/hashCode の互換性確認（変更しない）

## [x] 3. データベース移行（Flyway）
- [x] 3.1 既存マイグレーションから次バージョン番号を決定（V1 の次として V2, V3 を作成）
- [x] 3.2 Vx__create_customer_table.sql を追加（V2__create_customer_table.sql）
  - [x] 3.2.1 customer テーブル作成（id, version, 各カラム, created_date, updated_date, PK）
  - [x] 3.2.2 email 等のインデックス（必要に応じて）を追加（ix_customer_email）
- [x] 3.3 Vx__alter_beer_order_add_customer_id.sql を追加（V3__alter_beer_order_add_customer_id.sql）
  - [x] 3.3.1 beer_order に customer_id 列追加（NULL 許可）
  - [x] 3.3.2 外部キー制約（customer.id 参照）を追加
- [x] 3.4 空 DB に対して Flyway マイグレーションがクリーンに通ることを起動/テストで確認

## [x] 4. DTO（record）作成
- [x] 4.1 CustomerCreateRequest（必須項目に @NotBlank, email に @Email, 適切な @Size）
- [x] 4.2 CustomerUpdateRequest（全置換 PUT 前提で Create 同等の制約）
- [x] 4.3 CustomerResponse（id, version, timestamps 含む）

## [x] 5. MapStruct マッパー作成
- [x] 5.1 CustomerMapper インターフェースを追加
  - [x] 5.1.1 Customer toEntity(CustomerCreateRequest)
  - [x] 5.1.2 void updateEntity(@MappingTarget Customer, CustomerUpdateRequest)
  - [x] 5.1.3 CustomerResponse toResponse(Customer)
- [x] 5.2 必要に応じて nullValuePropertyMappingStrategy 等の設定検討（PUT は上書き）

## [x] 6. リポジトリ
- [x] 6.1 CustomerRepository（JpaRepository<Customer, Integer>）を追加
- [x] 6.2 追加 Finder: findByEmail(String email)（必要であれば）

## [x] 7. サービス層
- [x] 7.1 インターフェース CustomerService（package-private）を定義
  - [x] 7.1.1 CustomerResponse create(CustomerCreateRequest cmd)
  - [x] 7.1.2 Optional<CustomerResponse> getById(Integer id)
  - [x] 7.1.3 Page<CustomerResponse> list(Pageable pageable)
  - [x] 7.1.4 Optional<CustomerResponse> update(Integer id, CustomerUpdateRequest cmd)
  - [x] 7.1.5 boolean delete(Integer id)
- [x] 7.2 実装 CustomerServiceImpl を追加（コンストラクタ注入）
  - [x] 7.2.1 create/update/delete に @Transactional を付与
  - [x] 7.2.2 get/list に @Transactional(readOnly = true) を付与
  - [x] 7.2.3 バリデーション/存在確認（NotFound 例外 or Optional）を実装

## [x] 8. Web 層（REST コントローラ）
- [x] 8.1 ベースパス /api/v1/customers のコントローラを追加（package-private, コンストラクタ注入）
- [x] 8.2 POST /customers（201 Created, Location: /customers/{id}, body=CustomerResponse, @Valid）
- [x] 8.3 GET /customers/{id}（200 or 404）
- [x] 8.4 GET /customers（page,size,sort によるページング、200）
- [x] 8.5 PUT /customers/{id}（200 or 404, @Valid）
- [x] 8.6 DELETE /customers/{id}（204 or 404）

## [x] 9. 例外/エラーハンドリング
- [x] 9.1 @RestControllerAdvice の GlobalExceptionHandler を追加/拡張
- [x] 9.2 バリデーションエラー/Not Found を ProblemDetails 風レスポンスで返却
- [x] 9.3 Not Found: ResponseStatusException または CustomerNotFoundException をハンドル

## [x] 10. OpenAPI ドキュメント更新
- [x] 10.1 components/schemas に CustomerCreateRequest.yaml を追加
- [x] 10.2 components/schemas に CustomerUpdateRequest.yaml を追加
- [x] 10.3 components/schemas に CustomerResponse.yaml を追加
- [x] 10.4 paths/customers.yaml を追加（コレクション/アイテム操作を記述）
- [x] 10.5 openapi/openapi.yaml の paths に $ref を追記（file-per-path 規約）
- [x] 10.6 400/404 は components/responses/Problem 参照（既存 Problem.yaml に整合）
- [x] 10.7 redocly lint（openapi ディレクトリで npm ci → npm test）を実行しエラー解消

## [x] 11. テスト
- [x] 11.1 マッパー単体テスト（null ハンドリング含む）
- [x] 11.2 サービス単体テスト（リポジトリをモック、トランザクション/エッジケース）
- [x] 11.3 コントローラ・スライステスト（@WebMvcTest、@Valid と HTTP 契約）
- [x] 11.4 結合テスト（Testcontainers, RANDOM_PORT, リポジトリ〜コントローラ）
- [x] 11.5 OpenAPI リントテスト（redocly lint が CI/ローカルで成功）

## [x] 12. マイグレーション/互換性メモの反映
- [x] 12.1 beer_order.customer_id を初期は NULL 可で運用（後続で NOT NULL 化検討）
- [x] 12.2 既存の customerRef を当面維持（後続で廃止タスク化）
- [x] 12.3 必要ならバックフィル戦略を設計（別チケット）

## [x] 13. 仕上げとドキュメント
- [x] 13.1 README に新規エンドポイントを追記
- [x] 13.2 SLF4J ロギング確認（println があれば除去）
- [x] 13.3 package-private とコンストラクタ注入の遵守を全体レビュー

## [x] 14. マイルストーン進行（M1〜M7）
- [x] 14.1 M1: ドメイン + Flyway 完了
- [x] 14.2 M2: DTO + マッパー + リポジトリ 完了
- [x] 14.3 M3: サービス + トランザクション 完了
- [x] 14.4 M4: REST コントローラ + 例外処理 完了
- [x] 14.5 M5: OpenAPI 更新 + Lint 完了
- [x] 14.6 M6: テスト（単体/結合）完了
- [x] 14.7 M7: 仕上げ（README/ロギング/方針確認）完了
