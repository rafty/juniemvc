# add-dtos: 実行タスクリスト（チェックボックス付き・番号付き）

以下は `prompts/add-dtos/plan.md` の拡張計画に従った、詳細な実行タスクリストです。各項目は完了時に `[x]` を付けて進捗管理してください。番号はセクション内での順序を示します。

---

## 0. 前提とゴールの確認
[x] 0.1 既存の BeerController が Entity（Beer）を入出力に使用していることを再確認する
[x] 0.2 ゴール（DTO/Mapper 導入、Service/Controller の DTO 化、requirements.md 13章の受け入れ基準充足）を関係者と共有する

## 1. 現状分析と影響範囲の把握
[x] 1.1 対象コードの洗い出し（Controller/Entity/Repository/Service/テスト）
[x] 1.2 公開 API の入出力型変更による影響（外部クライアント/テスト）を整理する
[x] 1.3 Service の公開シグネチャ変更点を列挙する
[x] 1.4 テスト（Controller/Service）の修正必要箇所を特定する
[x] 1.5 pom.xml の MapStruct/Lombok 依存状況を確認する

## 2. 依存関係の確認/整備（必要時）
[x] 2.1 Lombok 依存が存在するか確認し、無ければ追加する
[x] 2.2 MapStruct 依存（mapstruct, mapstruct-processor）が存在するか確認し、無ければ追加する
[x] 2.3 maven-compiler-plugin の annotationProcessorPaths に mapstruct-processor と lombok を設定・確認する
[x] 2.4 IDE/CI のアノテーションプロセッサ有効化を確認する

## 3. DTO の追加（BeerDto）
[x] 3.1 パッケージ `guru.springframework.juniemvc.models` を作成する（無ければ）
[x] 3.2 BeerDto クラス/record を追加する
[x] 3.3 フィールドを定義する（id, version, beerName, beerStyle, upc, quantityOnHand, price, createdDate, updatedDate）
[x] 3.4 Lombok アノテーション（@Getter, @Setter, @Builder, @NoArgsConstructor, @AllArgsConstructor）を付与する
[x] 3.5 必要に応じて Jakarta Validation アノテーションの下地を用意する（例：@NotBlank, @Positive）
[x] 3.6 JSON シリアライズ（camelCase）前提で Jackson 互換を確認する

## 4. Mapper の追加（BeerMapper: MapStruct）
[x] 4.1 パッケージ `guru.springframework.juniemvc.mappers` を作成する（無ければ）
[x] 4.2 `@Mapper(componentModel = "spring")` で BeerMapper インターフェイスを定義する
[x] 4.3 メソッド `BeerDto toDto(Beer entity)` を定義する
[x] 4.4 メソッド `Beer toEntity(BeerDto dto)` を定義する
[x] 4.5 メソッド `void updateEntity(@MappingTarget Beer target, BeerDto source)` を定義する
[x] 4.6 DTO→Entity で `id`, `createdDate`, `updatedDate` を ignore するマッピングを設定する
[x] 4.7 必要に応じて `nullValuePropertyMappingStrategy = IGNORE` を検討・設定する（仕様に合わせる）

## 5. Service API の DTO 化
[x] 5.1 `BeerService` のシグネチャを DTO ベースに変更する
  [x] 5.1.1 `BeerDto create(BeerDto beerDto)`
  [x] 5.1.2 `Optional<BeerDto> getById(Integer id)`
  [x] 5.1.3 `List<BeerDto> listAll()`
  [x] 5.1.4 `Optional<BeerDto> update(Integer id, BeerDto beerDto)`
  [x] 5.1.5 `boolean delete(Integer id)`
[x] 5.2 `BeerServiceImpl` に `BeerMapper` をコンストラクタインジェクションで追加する
[x] 5.3 create: DTO→Entity（ignore 項目遵守）→ save → Entity→DTO で返却する実装に変更する
[x] 5.4 getById/listAll: 取得 Entity を DTO へ変換して返す
[x] 5.5 update: 既存取得 → `updateEntity` で上書き（id/createdDate/updatedDate は保持）→ save → DTO で返す
[x] 5.6 delete: 既存有無の判定方針を現行踏襲で実装を維持/調整する

## 6. Controller の DTO 化
[x] 6.1 メソッド署名を BeerDto ベースに変更する
  [x] 6.1.1 POST: `ResponseEntity<BeerDto>`（201 + Location + Body）
  [x] 6.1.2 GET by id: `ResponseEntity<BeerDto>`（200/404）
  [x] 6.1.3 GET list: `ResponseEntity<List<BeerDto>>`
  [x] 6.1.4 PUT: `ResponseEntity<BeerDto>`（200/404）
  [x] 6.1.5 DELETE: `ResponseEntity<Void>`（204/404）
[x] 6.2 既存 URL `/api/v1/beer` を維持する
[x] 6.3 Controller から Mapper を直接使用しない（Service に委譲）
[x] 6.4 可視性は可能な限り package-private、依存は final + コンストラクタインジェクションに統一する

## 7. バリデーション（任意・推奨）
[x] 7.1 BeerDto に妥当な Bean Validation を付与する（必要最小限）
[x] 7.2 Controller の `@RequestBody` に `@Valid` を付ける
[x] 7.3 検証エラー時は 400 を返す（詳細なレスポンス形式は将来の GlobalExceptionHandler に委ねる）

## 8. テストの更新
[x] 8.1 BeerControllerTest を DTO ベースに更新する
  [x] 8.1.1 モック Service が DTO を返すように修正する
  [x] 8.1.2 JSONPath 検証を BeerDto のプロパティに合わせて更新する
  [x] 8.1.3 ステータス/Location/Body の挙動を確認する
[x] 8.2 BeerServiceTest を更新する
  [x] 8.2.1 Mapper を含めた DTO↔Entity 変換（id/createdDate/updatedDate ignore）の要点を検証する
  [x] 8.2.2 Repository とのやりとりは現行のモック/スライス習慣を踏襲する
[x] 8.3 BeerRepositoryTest は直接影響なし（必要があれば最小限の修正のみに留める）

## 9. マイグレーションとコミット分割
[x] 9.1 feat: add BeerDto model（ビルド可能状態）
[x] 9.2 feat: add BeerMapper with MapStruct configuration
[x] 9.3 refactor(service): change BeerService API to use DTO + implementation
[x] 9.4 refactor(web): switch BeerController to DTO I/O, keep URLs unchanged
[x] 9.5 test: update controller/service tests to DTO-based assertions
[x] 9.6 chore: minor cleanups (visibility, constructor injection consistency)

## 10. リスクと緩和策の実施
[x] 10.1 MapStruct 生成失敗時の対処（processor 設定を pom で再確認）
[x] 10.2 `mvn -X -DskipTests=false clean test` でコード生成とテスト実行を検証する
[x] 10.3 後方互換（URL/JSON プロパティ名維持）を確認する
[x] 10.4 Null 上書きポリシーの決定とテスト担保（IGNORE を採用する場合は明記）

## 11. 完了条件（Definition of Done）の検証
[x] 11.1 BeerDto が `guru.springframework.juniemvc.models` に存在し、Lombok によりビルド可能
[x] 11.2 BeerMapper が `guru.springframework.juniemvc.mappers` に存在し、Spring Bean として機能
[x] 11.3 DTO→Entity 変換時に id/createdDate/updatedDate が無視されることを確認
[x] 11.4 Service 公開シグネチャが BeerDto ベースであり、内部で BeerMapper による相互変換を実施
[x] 11.5 BeerController が BeerDto を入出力に使用し、HTTP ステータスと Location ヘッダの挙動が従来どおり
[x] 11.6 DTO ベースのテストがグリーン（少なくとも本タスクで追加/更新した最小限のテストが通過）

## 12. ビルド/検証
[x] 12.1 `mvn clean verify` を実行し成功することを確認
[x] 12.2 ローカル/CI 双方でアノテーションプロセッサが動作していることを確認

## 13. コード規約とガイドラインの適用確認
[x] 13.1 コンストラクタインジェクションに統一、必須依存は `final` 化
[x] 13.2 可能な限り package-private を適用（Controller/Configuration など）
[x] 13.3 ログに SLF4J を使用し、System.out を使用していないことを確認
[x] 13.4 Web 層で Entity を直接返していないことを確認

## 14. フォローアップ（将来タスクの起票）
[ ] 14.1 `@RestControllerAdvice` によるグローバル例外ハンドリング（ProblemDetails 形式）のタスクを作成
[ ] 14.2 `spring.jpa.open-in-view=false`（OSIV 無効化）と必要なクエリ最適化のタスクを作成
[ ] 14.3 Pagination とレスポンスラッピング（メタ情報付与）導入タスクを作成
[ ] 14.4 i18n メッセージ整理（ResourceBundle）タスクを作成
