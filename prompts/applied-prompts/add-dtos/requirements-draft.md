BeerController を DTO を使うように更新してください。
models パッケージ内に、JPA Entity Beer と同じプロパティを持つ新しい POJO クラス BeerDto を作成します。
DTO には Project Lombok のアノテーション（@Builder を含む）を使用してください。
MapStruct の mapper を作成し、DTOとの相互変換を行えるようにします。
Mapper は mappers パッケージに追加してください。
DTOからJPAエンティティに変換する際は、id、createDate、updateDate のプロパティを無視してください。
サービス層を DTOオブジェクトを受け取り、MapStruct mapperを使用して型変換を行う ように変更します。
コントローラメソッドを更新し、新しい DTO POJO を入力およびサービスメソッド呼び出しに使用してください。