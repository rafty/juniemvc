# Customer 機能追加 — 実装計画（日本語版）

本計画は、prompts/add-customer/requirements.md に記載された要件を、このリポジトリと提供ガイドラインに適合させた、最小リスクかつ段階的に実行可能なロードマップへと具体化したものです。

## 0. 全体指針（すべてのステップに適用）
- 依存性注入はコンストラクタ注入のみ。必須依存は final フィールドで保持し、コンポーネントは必要がない限り package-private（デフォルト可視性）。
- Web 層と永続化層の分離：リクエスト／レスポンスは DTO（record）を使用し、マッピングは MapStruct を用いる。
- サービス層で明確なトランザクション境界を定義。参照系は readOnly。
- OSIV（Open Session in View）は無効化（spring.jpa.open-in-view=false を確認）。必要データは明示的にフェッチ。
- 例外処理は集中化し、ProblemDetails 互換レスポンスを返す。
- OpenAPI はリポジトリ既存の規約に従って更新：openapi/openapi/paths 配下は file-per-path、コンポーネントは openapi/openapi/components 配下。
- Flyway マイグレーションは src/main/resources/db/migration に配置し、Vx__description.sql の命名規則に従う。
- 結合テストは Testcontainers、SpringBootTest は RANDOM_PORT を使用。

## 1. ドメインモデル：BaseEntity と Customer エンティティ
1.1. BaseEntity の有無を確認
- 共通の mapped superclass を検索。存在しない（可能性高）場合、BeerOrder で用いられている id / version / 監査カラムを統一する BaseEntity を導入。
- 形（Jakarta Persistence）
  - id（Integer, @Id, @GeneratedValue IDENTITY）
  - version（Integer, @Version）
  - createdDate（@CreationTimestamp, updatable=false）
  - updatedDate（@UpdateTimestamp）
  - @MappedSuperclass
- 既存エンティティのリファクタは本変更では行わず、まず Customer のみで BaseEntity を採用。必要に応じて追跡課題を作成。

1.2. Customer エンティティを追加（BaseEntity を継承）
- テーブル名：customer
- 要件に基づくカラムと制約：
  - name（varchar 100–255, not null）
  - email（varchar 255, nullable、必要なら後続でインデックス）
  - phoneNumber（varchar 40, nullable）
  - addressLine1（varchar 255, not null）
  - addressLine2（varchar 255, nullable）
  - city（varchar 100, not null）
  - state（varchar 100, not null）
  - postalCode（varchar 20, not null）
- BeerOrder とのリレーション：OneToMany<Customer, BeerOrder> mappedBy "customer"。BeerOrder 側に Customer を指す ManyToOne を追加（既存データ移行まで nullable）。Fetch は LAZY、既定では cascade 無し。
- Lombok：既存エンティティに合わせて @Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor を付与。

1.3. BeerOrder エンティティを Customer に関連付け
- フィールドを追加：@ManyToOne(fetch = LAZY) @JoinColumn(name = "customer_id") private Customer customer;
- 互換性のため、既存フィールド（customerRef 等）は維持。
- 永続化上の問題回避のため、equals/hashCode（存在する場合）は変更しない。

リスクと緩和策：
- 既存データ：非 NULL 制約の FK を即時追加すると失敗するため、初期は FK を nullable にし、アプリ側のバリデーションで要件を担保。必要に応じてバックフィルの後、後続マイグレーションで NOT NULL を付与。

## 2. データベース移行（Flyway）
2.1. customer テーブル追加の Vx マイグレーションを作成
- パス：src/main/resources/db/migration/V<next>__create_customer_table.sql
- DDL 例：
  - create table customer (id serial/identity, version int, name varchar(..) not null, email varchar(..), phone_number varchar(..), address_line1 varchar(..) not null, address_line2 varchar(..), city varchar(..) not null, state varchar(..) not null, postal_code varchar(..) not null, created_date timestamp, updated_date timestamp, primary key (id));
  - 有用なインデックスを追加（例：DB が対応するなら idx_customer_email unique nulls not distinct、難しければ通常の非ユニークインデックス）。

2.2. beer_order.customer_id を追加するマイグレーションを作成
- alter table beer_order add column customer_id int null; foreign key (customer_id) references customer(id) を付与。
- DB が Postgres（可能性高）なら、既存マイグレーションに整合する generated identity 構文を用いる。

検証：
- ローカルでアプリ起動または Flyway テストを実行し、空 DB に対してクリーンに移行できることを確認。

## 3. API 設計と DTO
3.1. models パッケージに DTO（record）を追加
- CustomerCreateRequest：name, email, phoneNumber, addressLine1, addressLine2, city, state, postalCode
  - Jakarta Validation を付与：必須項目に @NotBlank、email に @Email、妥当な範囲で @Size
- CustomerUpdateRequest：create と同一項目。部分更新（PATCH）は任意、MVP では全置換（PUT）のみ対応。
- CustomerResponse：id, version, timestamps に加えて上記フィールド。必要なら将来、派生フィールドを追加可能。

3.2. MapStruct マッパー
- CustomerMapper のメソッド：
  - Customer toEntity(CustomerCreateRequest)
  - void updateEntity(@MappingTarget Customer, CustomerUpdateRequest)
  - CustomerResponse toResponse(Customer)
- 将来的に PATCH を追加する場合は nullValuePropertyMappingStrategy = IGNORE を設定。PUT では上書き。

## 4. リポジトリとサービス層
4.1. Spring Data リポジトリ
- interface CustomerRepository extends JpaRepository<Customer, Integer>
- 任意の Finder：findByEmail(String email)

4.2. サービス API と実装
- CustomerService インターフェース（package-private）とメソッド：
  - CustomerResponse create(CustomerCreateRequest cmd)
  - Optional<CustomerResponse> getById(Integer id)
  - Page<CustomerResponse> list(Pageable pageable, 追加フィルタは将来対応)
  - Optional<CustomerResponse> update(Integer id, CustomerUpdateRequest cmd)
  - boolean delete(Integer id) // 見つからない場合は false
- CustomerServiceImpl は CustomerRepository と CustomerMapper をコンストラクタ注入。
- トランザクション：
  - create/update/delete は @Transactional
  - 参照系（get/list）は @Transactional(readOnly = true)

## 5. Web 層（REST コントローラ）
- パスプレフィックス：/api/v1/customers
- エンドポイント（ResponseEntity）：
  - POST /customers → 201 Created、Location ヘッダ /customers/{id}、ボディは CustomerResponse
  - GET /customers/{id} → 200 または 404
  - GET /customers → ページング引数 page,size,sort → 200（リスト＋ページメタデータ、既存形式があればそれに合わせる）
  - PUT /customers/{id} → 200（更新後のリソース）または 404
  - DELETE /customers/{id} → 204 または 404
- バリデーション：@Valid をリクエストボディに付与。メソッド引数も検証。
- コントローラの可視性：package-private。コンストラクタ注入。

## 6. OpenAPI ドキュメント
6.1. スキーマ
- DTO とバリデーションに整合する components/schemas/CustomerCreateRequest.yaml, CustomerUpdateRequest.yaml, CustomerResponse.yaml を追加。

6.2. パス
- コレクション／アイテム操作を記述する paths/customers.yaml を追加。
- openapi/openapi/openapi.yaml の paths に $ref（file-per-path 規約）で配線。
- 400/404 は既存 components/schemas/Problem.yaml に整合する Problem レスポンス参照を使用。

6.3. セキュリティ
- 既存の securitySchemes があれば適用。デモ用途で公開の場合は未認証のままでも可。

## 7. 例外とエラーハンドリング
- 未整備の場合は @RestControllerAdvice の GlobalExceptionHandler を追加し、バリデーションエラー／Not Found を ProblemDetails 風構造で返却（既存 components/schemas/Problem.yaml を使用）。
- Not Found は ResponseStatusException を使用するか、CustomerNotFoundException を作成してマッピング。

## 8. テスト戦略
8.1. ユニットテスト
- マッパーテスト：マッピングと null ハンドリングを検証。
- サービステスト：リポジトリをモックし、トランザクション、挙動、エッジケースを検証。
- コントローラ・スライステスト（@WebMvcTest）：バリデーションと HTTP 契約をモックサービスで検証。

8.2. 結合テスト（Testcontainers）
- DB コンテナを起動（既存の BeerOrderIntegrationTest 構成に揃える）し、リポジトリ＋サービス＋コントローラのハッピーパスと 404 を検証。
- エンドツーエンドには RANDOM_PORT と RestClient/WebTestClient を使用。

8.3. OpenAPI リント
- openapi ディレクトリで npm ci および npm test を実行し、lint エラーが無いことを確認。

受け入れ基準（Acceptance criteria）：
- すべての新規／既存テストがローカルと CI で成功。
- Flyway が空 DB からクリーンにマイグレーション。
- CRUD エンドポイントが仕様どおり動作し、ドキュメント化されている。

## 9. マイグレーションと互換性メモ
- 既存注文への影響を避けるため、beer_order.customer_id は初期は nullable。今後は注文作成時に Customer 指定をアプリ側で必須化（バックフィルの後、後続マイグレーションで FK に NOT NULL を付与可能）。
- 互換保持のため、BeerOrder の customerRef は当面維持し、後で廃止予定。

## 10. 作業分割と順序（マイルストーン）
M1. ドメイン＋Flyway
- BaseEntity（未存在なら）と Customer エンティティを追加。
- Flyway マイグレーション（customer テーブル、beer_order.customer_id FK）。

M2. リポジトリ＋マッパー＋DTO
- DTO、MapStruct マッパー、リポジトリを実装。

M3. サービス＋トランザクション
- トランザクションとバリデーションを備えたサービスインターフェース＋実装を追加。

M4. REST コントローラ＋例外処理
- コントローラを実装し、GlobalExceptionHandler が無ければ配線。

M5. OpenAPI
- スキーマとパスを追加し、openapi.yaml に参照を設定。redocly lint を実行。

M6. テスト
- ユニット（マッパー、サービス）とコントローラ・スライス、Testcontainers を用いた結合テストを実施。RANDOM_PORT を確認。

M7. 仕上げとドキュメント
- 必要に応じて README を更新（新規エンドポイント）。SLF4J によるロギングを確認し、println があれば除去。package‑private とコンストラクタ注入を確認。

## 11. リスクと緩和
- 既存 DB とのスキーマドリフト：新規マイグレーションに変更を閉じ込め、FK は初期 nullable、インデックスは慎重に追加。
- コンテナ利用の不安定さ：イメージのバージョンを固定し、可能なら再利用可能コンテナを使用。
- MapStruct 生成の問題：アノテーションプロセッサが有効（BeerOrderMapper で既に使用）であることを確認。

## 12. 完了の定義（Definition of Done）
- Customer エンティティが永続化され、BeerOrder と連携している。
- CRUD REST エンドポイントが提供され、ドキュメント化されている。
- すべてのテスト（ユニット、結合、OpenAPI リント）が成功。
- コードは提供された Spring Boot ガイドライン（コンストラクタ注入、package‑private、DTO 分離、トランザクション、ロギング、OSIV 無効）に準拠。
