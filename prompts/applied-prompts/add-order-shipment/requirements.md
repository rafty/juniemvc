## 変更要件（Change Requirements）

プロジェクトに BeerOrderShipment という新しいエンティティを追加してください。

BeerOrderShipment エンティティは次のプロパティを持ちます：

* shipmentDate - not null（必須）
* carrier
* trackingNumber

BeerOrderShipment エンティティは BaseEntity を継承する必要があります。
また、BeerOrder エンティティとは 一対多（OneToMany）関係を持ちます。

新しい BeerOrderShipment JPA エンティティ用の Flyway マイグレーションスクリプトを追加してください。

コントローラの操作パスは次のようにします：

```
/api/v1/beer-orders/{beerOrderId}
```

ここで beerOrderId は親である BeerOrder エンティティの id です。

コントローラおよびサービスは、BeerOrderShipment が属する BeerOrder の id を受け取る必要があります。

Java DTO、Mapper、Spring Data Repository、Service、および Service 実装クラスを追加し、
Spring MVC の RESTful CRUD コントローラをサポートしてください。

また、すべてのコンポーネントに対する テストを追加し、
新しいコントローラ操作について OpenAPI ドキュメントを更新してください。

すべてのテストが通過することを確認してください。