# 新規開発者向けガイドライン（junie mvc）

このドキュメントは、プロジェクト構造と技術スタックの概要、セットアップ、ビルド/実行、テスト、日常作業、ベストプラクティスを簡潔にまとめたものです。迷ったらここを見てください。

## 1. プロジェクト概要
- フレームワーク: Spring Boot 3.5.x（Java 21）
- ビルド: Maven（mvnw ラッパー同梱）
- 永続化: Spring Data JPA + H2（ランタイム）、Flyway（マイグレーション）
- その他: Bean Validation、Lombok、MapStruct
- メイン機能サンプル: Beer エンティティの CRUD REST API
  - ベースパス: /api/v1/beer

## 2. ディレクトリ構造（主要）
- pom.xml: 依存関係・ビルド設定（Java 21、MapStruct、Lombok 等）
- src/main/java/...: アプリ本体
  - controllers/BeerController.java: REST エンドポイント
  - entities/Beer.java: JPA エンティティ
  - repositories/BeerRepository.java: Spring Data JPA リポジトリ
  - services/*: サービス層（BeerService / Impl）
- src/main/resources/application.properties: アプリ設定
- src/test/java/...: ユニット/スライス/統合テスト
- mvnw, mvnw.cmd: Maven ラッパー（ローカル Maven 不要）

## 3. 事前準備
- 必須: JDK 21（JAVA_HOME 設定推奨）
- 推奨 IDE: IntelliJ IDEA（Lombok アノテーションの処理を有効化）
- ネットワーク: 依存取得のため外部 Maven Central にアクセス可能であること

## 4. よく使うコマンド（すべてプロジェクトルートで実行）
- 依存取得＋ビルド（テスト込み）:
  - macOS/Linux: ./mvnw clean verify
  - Windows: mvnw.cmd clean verify
- アプリ起動（開発時）:
  - ./mvnw spring-boot:run
- パッケージング（fat jar 作成）:
  - ./mvnw clean package
  - 実行: java -jar target/juniemvc-0.0.1-SNAPSHOT.jar
- テスト実行:
  - 全テスト: ./mvnw test
  - テストクラス単体: ./mvnw -Dtest=guru.springframework.juniemvc.controllers.BeerControllerTest test
  - 1テストメソッドのみ: ./mvnw -Dtest="FQN#methodName" test

## 5. API の動作確認（例）
- 起動後、以下で確認可能（デフォルトは http://localhost:8080）
  - POST /api/v1/beer で作成
  - GET /api/v1/beer/{id} で取得
  - GET /api/v1/beer で一覧
  - PUT /api/v1/beer/{id} で更新
  - DELETE /api/v1/beer/{id} で削除

## 6. データベース/マイグレーション
- デフォルトは H2（インメモリ/ローカル）。本番系設定や Flyway マイグレーションスクリプトは適宜追加してください。
- Flyway は依存済み。V1__*.sql 等を src/main/resources/db/migration に配置すると起動時に適用されます。

## 7. コーディングと設計の方針
- レイヤリング: Controller → Service → Repository の依存方向を維持
- バリデーション: @Valid / Bean Validation を活用（DTO 導入時は特に有効）
- 例外/戻り値: 404 等の適切な HTTP ステータスを返す（BeerController 参照）
- Lombok: @Getter/@Setter/@Builder 等の利用時、IDE の annotation processing を ON
- MapStruct: マッピングが増えたら interface + @Mapper で定義（processor は pom に設定済み）

## 8. テスト戦略
- 単体テスト: サービスやコントローラの振る舞いを迅速に検証
- スライステスト/統合テスト: Spring Boot Test（spring-boot-starter-test 依存あり）
- 実行コマンドは「4. よく使うコマンド」を参照
- テスト命名/粒度: 失敗時に原因がわかるように Given-When-Then を意識

## 9. スクリプト/ユーティリティ
- 専用スクリプトは現時点なし。Maven ラッパー（mvnw）が共通エントリポイント
- 将来的にスクリプトを追加する場合は scripts/ 配下を作成し、README か本ガイドに追記

## 10. Git 運用とコミット
- ブランチ: トピックブランチ運用（例: feature/add-xyz, fix/bug-123）
- コミットメッセージ（例）:
  - feat: add BeerController CRUD endpoints
  - fix: handle 404 on beer update when id not found
  - test: add service tests for beer listing
  - chore: bump mapstruct to 1.6.3
- Pull Request: 目的、変更点、テスト観点、動作確認手順を簡潔に記載

## 11. ベストプラクティス
- 小さく頻繁にコミット／PR（レビュー容易化）
- 設計/命名は「意図が伝わること」を最優先
- ドメインロジックはサービス層へ集約し、Controller は薄く
- Null 安全・Optional の活用、早期 return で分岐を簡潔に
- ログは必要最小限・情報価値の高いものに限定

## 12. トラブルシューティング
- Java バージョン不一致: java -version が 21 であることを確認
- 依存解決失敗: ネットワークと Maven Central へのアクセス権を確認、./mvnw -U で更新
- ポート競合: 8080 が使用中なら、server.port=0 を application.properties に追加して回避
- Lombok の警告: IntelliJ の「Annotation Processing」を有効化

## 13. 追加リソース
- Spring Boot Docs: https://docs.spring.io/spring-boot
- MapStruct: https://mapstruct.org/
- Lombok: https://projectlombok.org/
- Flyway: https://flywaydb.org/

以上。詳細はソースとテスト（src/test/java）を併読してください。