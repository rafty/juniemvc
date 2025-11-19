# プロンプト変数（Prompt Variables）
以下の変数をプロンプト内のプレースホルダーに適用します。  
プレースホルダーは `${variable}` という構文で示されます。

# プレースホルダーの定義（Placeholders Definitions）
以下のキーと値のペアは、プロンプト内でプレースホルダーを置き換えるために使用されます。  
形式は「変数名 = 値」のペアとして定義されます。

- `entity_name` = `FooEntity`
- `property_name` = `page`
- `property_type` = `type`

## タスクの説明（Task Description）
あなたのタスクは、JPAエンティティ `${entity_name}` に新しいプロパティを追加することです。  
追加するプロパティの名前は `${property_name}` で、型は `${property_type}` です。

### 以下の作業を完了してください：
* 新しい Flyway マイグレーションスクリプトを作成し、データベーステーブルにプロパティを追加すること。
* JPA エンティティに `${property_name}` という名前の新しいプロパティを追加すること。
* 対応する DTO に `${property_name}` という名前の新しいプロパティを追加すること。
* OpenAPI ドキュメントを更新し、新しいプロパティ `${property_name}` を反映させること。
* すべてのテストがパスしていることを確認すること。
* OpenAPI 仕様が有効であることを確認すること。  
