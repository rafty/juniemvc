# プロンプト変数（Prompt Variables）
以下の変数をプロンプト内のプレースホルダーに適用してください。  
プレースホルダーは `${variable}` の構文で表されます。  

# プレースホルダーの定義（Placeholders Definitions）
以下のキーと値のペアは、プロンプト内のプレースホルダーを置き換えるために使用します。
`variable` は変数名を、`value` はそのプレースホルダーを置き換える値を表します。
次の一覧では、`variable name` = `value` の形式で定義されています：

* controller_name = `FooController`
* parameter_name = `page`

## タスク説明（Task Description）
あなたのタスクは、既存のコントローラの **list（一覧取得）操作にクエリパラメータを実装すること**です。
すべての Java コードを実装した後、OpenAPI ドキュメントを更新して変更内容を反映してください。
`.junie/guidelines.md` のガイドラインを使用してください。

### タスク手順（Task Steps）

* 既存のコントローラ `${controller_name}` を確認し、list 操作を特定する。
* 既存の list 操作を修正し、Spring MVC / Spring Data を用いてクエリパラメータ `${parameter_name}` を受け取れるようにする。
* `${parameter_name}` のデータ型は、返却される DTO 内の対応するプロパティと一致させること。
* クエリパラメータはオプションとし、指定されなかった場合は `null` をデフォルトとする。
* 既存の list 操作を更新し、新しい操作や新しい API パスは作成しないこと。
* コントローラの list 操作は、既存パッケージ `org.springframework.data.domain` の `Page<T>` オブジェクトを返すこと。
* サービス層を更新し、list 操作で `${parameter_name}` をサポートできるようにする。
  list メソッドに `${parameter_name}` を受け取る引数を追加する。
* サービス層では、クエリパラメータが `null` または空の場合があるオプション値として扱う必要がある。
* Spring Data Repository に新しいメソッドを追加し、クエリパラメータをサポートする `findAll` を実装する。
  このメソッドは `Pageable` と `${parameter_name}` を受け取り、`Page<T>` を返すこと。
* ページングに必須である `page` と `size` を除き、他の任意パラメータについては **任意の組み合わせ（null や空を含む）に対応できるロジック**を追加する。
  例：

    * parameter1 と parameter2 の両方あり
    * parameter1 のみ
    * parameter2 のみ
    * 両方 null または空
      といった柔軟な組み合わせをサポートする。
* コントローラ、サービス実装、リポジトリについて、**新しいページング機能をカバーする単体テストを更新**する。
* コントローラ、サービス実装、リポジトリについて、**ページング機能をカバーする新しい単体テストを作成**する。
* 更新された単体テストがすべて通過することを確認する。
* コントローラに対して行った変更を反映するよう **OpenAPI ドキュメントを更新**する。
* OpenAPI ドキュメント内の list 操作のパラメータ定義を更新し、新しいクエリパラメータを追加する。
* OpenAPI ドキュメントが有効であることを確認する。
