## Change Requirements
Add a new entity to the project called Customer.

The Customer entity has the following properties:
* name - not null
* email
* phone number
* address line 1 - not null
* address line 2
* city - not null
* state - not null
* postal code - not null

The Customer entity should extend the BaseEntity has a OneToMany relationship with BeerOrder.

Add a flyway migration script for the new Customer JPA Entity.

Add Java DTOs, Mappers, Spring Data Repositories, service and service implementation to support a Spring MVC RESTful
CRUD controller. Add Tests for all components. Update the OpenAPI documentation for the new controller operations. Verify
all tests are passing.

---
## 変更要件（Change Requirements）

プロジェクトに **Customer** という新しいエンティティを追加すること。

### Customer エンティティのプロパティ

以下のプロパティを持つものとする：

* **name**（必須 / not null）
* **email**
* **phone number**
* **address line 1**（必須 / not null）
* **address line 2**
* **city**（必須 / not null）
* **state**（必須 / not null）
* **postal code**（必須 / not null）

### エンティティ要件

* **Customer エンティティは BaseEntity を継承すること。**
* **BeerOrder と 1 対多（OneToMany）のリレーションを持たせること。**

### マイグレーション

* 新しい **Customer JPA エンティティに対応する Flyway マイグレーションスクリプト**を追加すること。

### 実装コンポーネント

以下を追加・実装すること：

* Java **DTO**
* **Mapper**
* **Spring Data Repository**
* **Service と Service Implementation**
* 上記を利用する **Spring MVC の RESTful CRUD コントローラ**

### テスト・ドキュメント

* すべてのコンポーネントに対して **テストを追加**すること。
* 新しいコントローラ操作に対応する **OpenAPI ドキュメントを更新**すること。
* **すべてのテストが成功することを確認**すること。
