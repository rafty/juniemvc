# add-dtos: 実装計画（詳細）

本計画は `prompts/add-dtos/requirements.md` を満たし、既存の Beer API に DTO/Mapper を導入して Web 層と Persistence 層の分離を図るための具体的な作業手順を示します。Spring ガイドライン（コンストラクタインジェクション、package-private、REST の一貫性など）に準拠します。

---

## 0. 前提とゴール
- 既存の BeerController は JPA Entity（Beer）を入出力に使用している。
- 目標: BeerDto と BeerMapper（MapStruct）を導入し、Service/Controller を DTO ベースに移行する。
- 受け入れ基準: requirements.md の「13. 受け入れ基準」に完全適合。ビルド/テストが通ること。

---

## 1. 現状分析と影響範囲
- 対象コード
  - Controller: `guru.springframework.juniemvc.controllers.BeerController`
  - Entity: `guru.springframework.juniemvc.entities.Beer`
  - Repository: `guru.springframework.juniemvc.repositories.BeerRepository`
  - Service: `guru.springframework.juniemvc.services.BeerService` / `impl.BeerServiceImpl`
  - テスト: Controller/Service/Repository の各テスト
- 影響範囲
  - Public API の入出力型変更（Entity -> DTO）
  - Service の公開シグネチャ変更
  - Controller/Service テストの修正
  - MapStruct/Lombok の依存（pom.xml）確認

---

## 2. 依存関係の確認/整備（必要時のみ）
- Lombok と MapStruct の依存が無い場合に追加（requirements.md 11章参照）。
- maven-compiler-plugin の annotationProcessorPaths に mapstruct-processor, lombok を設定。
- CI/IDE でアノテーションプロセッサが有効であることを確認。

---

## 3. DTO の追加（BeerDto）
- パッケージ: `guru.springframework.juniemvc.models`
- フィールド（requirements.md 5章の推奨）
  - Integer id, Integer version, String beerName, String beerStyle, String upc,
    Integer quantityOnHand, BigDecimal price, LocalDateTime createdDate, LocalDateTime updatedDate
- アノテーション
  - Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
  - 余地: Jakarta Validation（例: `@NotBlank beerName`, `@Positive price`）
- JSON: camelCase 前提で Jackson デフォルトを利用。

---

## 4. Mapper の追加（BeerMapper: MapStruct）
- パッケージ: `guru.springframework.juniemvc.mappers`
- 宣言:
  - `@Mapper(componentModel = "spring")`
- メソッド:
  - `BeerDto toDto(Beer entity)`
  - `Beer toEntity(BeerDto dto)`
  - `void updateEntity(@MappingTarget Beer target, BeerDto source)`
- マッピングルール（DTO -> Entity）
  - `id`, `createdDate`, `updatedDate` は ignore（サーバ生成/管理）
  - 必要に応じて `nullValuePropertyMappingStrategy = IGNORE`（PUT の上書きポリシーに合わせる）

---

## 5. Service API の DTO 化
- 変更前
  - `Beer create(Beer beer)` / `Optional<Beer> getById(Integer id)` / `List<Beer> listAll()`
  - `Optional<Beer> update(Integer id, Beer beer)` / `boolean delete(Integer id)`
- 変更後
  - `BeerDto create(BeerDto beerDto)`
  - `Optional<BeerDto> getById(Integer id)`
  - `List<BeerDto> listAll()`
  - `Optional<BeerDto> update(Integer id, BeerDto beerDto)`
  - `boolean delete(Integer id)`
- 実装変更（BeerServiceImpl）
  - 依存: `BeerRepository`, `BeerMapper` をコンストラクタインジェクション
  - create: DTO -> Entity（ignore項目遵守） -> save -> Entity -> DTO で返却
  - getById/listAll: Entity -> DTO 変換
  - update: 既存取得 -> updateEntity で上書き（id/createdDate/updatedDate は保持）-> save -> DTO
  - delete: 既存有無確認の方針は現行踏襲

---

## 6. Controller の DTO 化
- メソッド署名を BeerDto ベースへ変更
  - POST: `ResponseEntity<BeerDto>`（201 + Location + Body）
  - GET by id: `ResponseEntity<BeerDto>`（200/404）
  - GET list: `ResponseEntity<List<BeerDto>>`
  - PUT: `ResponseEntity<BeerDto>`（200/404）
  - DELETE: `ResponseEntity<Void>`（204/404）
- 注意
  - URL は既存 `/api/v1/beer` を踏襲
  - Controller では Mapper を直接使用しない（Service に委譲）
  - 可視性: 可能な限り package-private、依存は final + コンストラクタインジェクション

---

## 7. バリデーション（任意・推奨）
- BeerDto に `@NotBlank`, `@PositiveOrZero` などを付与
- Controller の `@RequestBody` に `@Valid` を付加
- 検証エラーは 400（詳細フォーマットは今後の GlobalExceptionHandler で統一）

---

## 8. テストの更新
- BeerControllerTest
  - モック Service を DTO ベースへ変更
  - JSONPath 検証を BeerDto のプロパティに合わせて更新
  - ステータス/Location/Body の挙動を確認
- BeerServiceTest
  - Mapper を含めた DTO<->Entity 変換の要点（id/createdDate/updatedDate ignore）を検証
  - Repository とのやり取りは現行踏襲のモック/スライスに合わせる
- BeerRepositoryTest
  - 直接の影響なし（Entity 層そのまま）

---

## 9. マイグレーション手順とコミット分割案
1) feat: add BeerDto model（ビルド可能）
2) feat: add BeerMapper with MapStruct configuration
3) refactor(service): change BeerService API to use DTO + implementation
4) refactor(web): switch BeerController to DTO I/O, keep URLs unchanged
5) test: update controller/service tests to DTO-based assertions
6) chore: minor cleanups (visibility, constructor injection consistency)

---

## 10. リスクと緩和策
- MapStruct 生成失敗
  - 対策: processor 設定を pom で確認、`mvn -X -DskipTests=false clean test` で検証
- 互換性問題（外部クライアント）
  - URL/プロパティ名を維持し、JSON 構造は Beer と同等フィールドで後方互換を担保
- Null 上書きポリシー
  - `NullValuePropertyMappingStrategy.IGNORE` を採用する場合、仕様に明記しテストで担保

---

## 11. 完了条件（Definition of Done）
- requirements.md の 13章 Acceptances に合致
- `mvn clean verify` が成功
- テストは DTO ベースでグリーン
- コード規約: コンストラクタインジェクション、SLF4J、package-private 適用（可能な限り）

---

## 12. 参考設定（pom.xml サンプル追記内容：必要時）
- dependencies:
  - `org.mapstruct:mapstruct`
  - `org.mapstruct:mapstruct-processor`
  - `org.projectlombok:lombok`
- maven-compiler-plugin の `annotationProcessorPaths` に `mapstruct-processor` と `lombok` を定義

---

## 13. 作業見積もり
- 実装: 2–4 時間
- テスト更新: 1–2 時間
- ビルド/デバッグ: 0.5–1 時間

---

## 14. フォローアップ（将来タスク）
- `@RestControllerAdvice` によるグローバル例外ハンドリング（ProblemDetails 形式）
- OSIV 無効化とクエリ最適化（fetch join/entity graph）
- Pagination の導入とレスポンスラッピング（メタ情報含む）
- i18n メッセージ整理（ResourceBundle）
