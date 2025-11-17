# プロンプト変数（Prompt Variables）
以下の変数をプロンプト内のプレースホルダーに適用してください。  
プレースホルダーは `${variable}` の構文で表されます。  

# プレースホルダーの定義（Placeholders Definitions）
以下のキーと値のペアは、プロンプト内のプレースホルダーを置き換えるために使用されます。  
`variable` が変数名、`value` がそのプレースホルダーを置き換える値です。  
次の一覧では、`変数名` = `値` の形式で定義されています：  

* controller_name = `FooController`
* parameter_name = `page`

## タスクの説明（Task Description）
todo タスク `${controller_name}` を追加すること。  
