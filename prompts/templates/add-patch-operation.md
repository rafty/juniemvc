# プロンプト変数（Prompt Variables）
以下の変数をプロンプト内のプレースホルダーに適用します。  
プレースホルダーは `${variable}` という構文で表されます。

# プレースホルダーの定義（Placeholders Definitions）
以下のキーと値のペアは、プロンプト内でプレースホルダーを置き換えるために使用されます。  
形式は「変数名 = 値」のペアとして定義されます。

- `controller_name` = `FooController`

## タスクの説明（Task Description）
あなたのタスクは、Spring MVC コントローラー `${controller_name}` に Patch 操作を追加することです。

### このタスクを完了するために、以下の手順を実施してください：
* `<EntityName>PatchDto` という命名規則を使用して、新しい Patch Operation 用の DTO を作成すること。
* 新しい Patch DTO には、`@NotNull` または `@NotBlank` の制約を付与しないこと。
* MapStruct マッパーに、`@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`  
  を付与した `update` メソッドを追加し、**null 値を無視して更新**を行うようにすること。
* Patch DTO の値を使用して既存のエンティティを更新する新しいサービスメソッドを作成すること。
* Patch 操作用に、マッパー、サービス、および MockMVC の追加テストを作成すること。
* 新しい操作および DTO に対応する OpenAPI ドキュメントを更新すること。
* すべてのテストがパスしていることを確認すること。
* OpenAPI 仕様が有効であることを確認すること。  
