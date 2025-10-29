# add-dtos: 改善済み要件

このドキュメントは、Beer API に DTO を導入し、Web 層と永続化層の分離を強化するための実装要件を定義します。設計上の意図、非目標、パッケージ配置、マッピング仕様、API 変更点、テスト観点、受け入れ基準を明確に示します。

## 1. 背景と目的
- 現状の BeerController は JPA Entity (Beer) を直接入出力している。
- ガイドライン「Web層とPersistence層を分離する」に従い、公開 API では DTO を使用する。
- MapStruct による型マッピングを採用し、変換処理の可読性と保守性を高める。

## 2. スコープ
- Beer 用の DTO の新規作成。
- MapStruct マッパーの新規作成。
- Service 層のシグネチャを DTO ベースに変更し、マッピング導入。
- Controller の入出力を DTO に変更。
- Lombok を使った DTO の実装（@Builder を含む）。

### 非スコープ（本タスクでは実装しないが将来推奨）
- グローバル例外ハンドラ（ProblemDetails/RFC 9457 への統一）。
- 入力バリデーション詳細の拡充（Bean Validation の厳密定義やメッセージ多言語化）。
- OSIV 無効化設定やクエリ最適化。

## 3. 用語
- Entity: JPA 永続化用のドメインオブジェクト（Beer）。
- DTO: Web/API での入出力用データ転送オブジェクト（BeerDto）。
- Mapper: Entity と DTO の相互変換を行う MapStruct のインターフェイス。

## 4. パッケージとクラス配置
- DTO: `guru.springframework.juniemvc.models.BeerDto`
- Mapper: `guru.springframework.juniemvc.mappers.BeerMapper`
- 既存 Entity: `guru.springframework.juniemvc.entities.Beer`（変更なし）
- 既存 Service: `guru.springframework.juniemvc.services.*`（シグネチャ変更）
- 既存 Controller: `guru.springframework.juniemvc.controllers.BeerController`（入出力変更）

可視性方針（ガイドライン準拠）
- Controller/Configuration/Bean は可能な限り package-private とする。ただしテストや Spring の可視性要件で問題があれば public のままでも可。
- 依存注入はコンストラクタインジェクションを用いる（@Autowired はコンストラクタが1本なら不要）。

## 5. DTO 定義（BeerDto）
Entity `Beer` の公開が必要なプロパティを DTO に定義する。

推奨フィールド（Entity と同名、camelCase）
- Integer id
- Integer version
- String beerName
- String beerStyle
- String upc
- Integer quantityOnHand
- BigDecimal price
- LocalDateTime createdDate
- LocalDateTime updatedDate

実装要件
- Lombok を使用すること。
  - 最低限: `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Jackson により JSON シリアライズ/デシリアライズ可能であること（標準設定の camelCase 前提）。
- 将来の検証追加を見据え、必要に応じて Bean Validation アノテーションを付与可能な構造にする。

備考
- 入力（POST/PUT）では id/createdDate/updatedDate はクライアントから指定されない/無視される場合があるため、後述のマッピング規則に従う。

## 6. マッピング仕様（BeerMapper: MapStruct）
- パッケージ: `guru.springframework.juniemvc.mappers`
- インターフェイス: `BeerMapper`
- 実装は MapStruct により自動生成（`@Mapper(componentModel = "spring")`）。

メソッド
- BeerDto toDto(Beer entity)
- Beer toEntity(BeerDto dto)
- void updateEntity(@MappingTarget Beer target, BeerDto source) — PUT 時の上書き用（null は上書きしない方針を採用するかは要件次第。デフォルトは「null でも上書き」でもよいが、必要であれば `NullValuePropertyMappingStrategy.IGNORE` を設定）。

マッピングルール
- DTO -> Entity 変換時は以下のプロパティを無視すること：
  - id
  - createdDate
  - updatedDate
  これらはサーバ側で生成・管理する（Service/Repository 層）。
- version の扱い：
  - 新規作成（create）では無視または null を許容。既存更新（update）では DTO の version を Entity に反映してもよいが、楽観ロック方針は別要件とする。

MapStruct 設定例
- `@Mapper(componentModel = "spring")`
- DTO -> Entity のメソッドに `@Mapping(target = "id", ignore = true)`, `@Mapping(target = "createdDate", ignore = true)`, `@Mapping(target = "updatedDate", ignore = true)`
- 必要に応じて `nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE`

## 7. Service 層の変更
現在の Service API（例）
- Beer create(Beer beer)
- Optional<Beer> getById(Integer id)
- List<Beer> listAll()
- Optional<Beer> update(Integer id, Beer beer)
- boolean delete(Integer id)

変更後の API（DTO ベース）
- BeerDto create(BeerDto beerDto)
- Optional<BeerDto> getById(Integer id)
- List<BeerDto> listAll()
- Optional<BeerDto> update(Integer id, BeerDto beerDto)
- boolean delete(Integer id)

実装要件
- Service 実装では BeerMapper をコンストラクタインジェクションで受け取り、DTO と Entity の相互変換に利用する。
- create:
  - 受け取った DTO から Entity を生成（id/createdDate/updatedDate は無視）。
  - Repository 保存後の Entity を DTO に変換して返す。
- getById/listAll:
  - 取得した Entity を DTO に変換して返す。
- update:
  - id で既存 Entity を検索。
  - 見つからない場合は Optional.empty。
  - 見つかった場合、DTO -> Entity で上書き（id/createdDate/updatedDate は保持）。
  - 保存後の Entity を DTO に変換して返す。

## 8. Controller の変更
エンドポイントは現行の URL を維持（後方互換のため）。入出力型のみ DTO へ変更。

- POST /api/v1/beer
  - Request Body: BeerDto（id/createdDate/updatedDate はリクエストで無視または未指定）
  - Response: 201 Created + Location ヘッダ + Body: BeerDto（作成後の id 等を含む）

- GET /api/v1/beer/{id}
  - Response: 200 OK + BeerDto
  - Not Found: 404

- GET /api/v1/beer
  - Response: 200 OK + List<BeerDto>

- PUT /api/v1/beer/{id}
  - Request Body: BeerDto（上記マッピングルールに従う）
  - Response: 200 OK + BeerDto（更新結果）
  - Not Found: 404

- DELETE /api/v1/beer/{id}
  - Response: 204 No Content（成功）/ 404 Not Found（対象なし）

その他要件
- メソッドは `ResponseEntity<...>` を返し、明示的にステータスコードを制御。
- JSON プロパティは camelCase を維持。
- Controller でも MapStruct は直接使用せず、Service のみで使用する（関心分離）。

## 9. バリデーション（推奨）
- DTO に Jakarta Validation アノテーションを付与可能にする（例：`@NotBlank beerName`, `@Positive price` 等）。
- Controller の入力では `@Valid` を付け、検証エラーは 400 とする（詳細なエラー形式は将来のグローバルハンドラで統一）。

## 10. 実装指針（ガイドライン準拠）
- 依存注入：コンストラクタインジェクション、必須依存は `final`。
- 可視性：可能な限り package-private。
- ログ：SLF4J を使用、System.out は使用しない。
- DTO を公開 API に使用し、Entity を直接返さない。

## 11. ビルド/依存
- MapStruct と Lombok は既に依存がある前提。なければ以下を追加（例）：
  - Lombok: `org.projectlombok:lombok`
  - MapStruct: `org.mapstruct:mapstruct`, `org.mapstruct:mapstruct-processor`
  - `maven-compiler-plugin` の annotationProcessorPaths に processor を設定。

## 12. テスト観点
- 既存の Controller テストは Entity 前提になっているため、DTO 用に更新する。
  - モックする Service は DTO を返すように修正。
  - JSON パスの検証は BeerDto のプロパティに合わせる。
- Service 単体テストでは Mapper の動作を含め、DTO <-> Entity 変換が意図どおり（id/createdDate/updatedDate の扱い）であることを検証。

## 13. 受け入れ基準（Acceptance Criteria）
- BeerDto が `guru.springframework.juniemvc.models` に定義され、Lombok によりビルド可能。
- BeerMapper が `guru.springframework.juniemvc.mappers` に定義され、Spring Bean として使用可能。
- DTO -> Entity 変換時に id/createdDate/updatedDate が無視されるマッピングが実装されている。
- Service の公開シグネチャが BeerDto ベースに変更され、内部で BeerMapper により相互変換している。
- BeerController が入出力として BeerDto を使用し、HTTP ステータスと Location ヘッダの挙動が従来どおりである。
- テストが DTO ベースで通過する（もしくは本タスクの範囲で用意した最小限のテストが成功する）。

## 14. マイグレーション手順（参考）
1) BeerDto を追加。
2) BeerMapper を追加。
3) BeerService/BeerServiceImpl のシグネチャと実装を DTO ベースに変更。
4) BeerController の入出力を DTO に変更。
5) 既存テストを DTO 前提に修正。
6) コンパイル・テスト実行。

## 15. 注意事項
- エンドポイントの URL と意味論は変更しない（後方互換）。
- 例外処理の形式統一や i18n は別タスクで実施予定（本ドキュメントはその前提に配慮）。
- 将来的に Pagination 導入時は、DTO のコレクションをページングレスポンス（オブジェクト）でラップする方針。
