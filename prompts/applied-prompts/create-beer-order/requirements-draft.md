### ERD の読み取り結果（関係まとめ）
- エンティティ: `BeerOrder`, `BeerOrderLine`, `Beer`
- 主キー: 各テーブルとも `id`（Integer）
- 関係:
    - `BeerOrder 1 — * BeerOrderLine`
        - `BeerOrderLine` は親 `BeerOrder` に属する（多対一）。
    - `Beer 1 — * BeerOrderLine`
        - `BeerOrderLine` は 1 つの `Beer` を参照（多対一）。
- 監査系: すべてに `createdDate`, `updateDate`
- 楽観ロック: `version`
- 金額: `paymentAmount`（`BeerOrder`）、`price`（`Beer`）→ `BigDecimal`
- ステータス: 各テーブルに `status`（String）→ Enum 化を推奨（`@Enumerated(STRING)`）

---

### 実装方針（ガイドライン準拠）
- Lombok は `@Getter/@Setter` を基本に使用。JPA では `@Data` は避ける（`equals/hashCode` と `toString` が双方向関連で暴走しやすい）。
- `equals/hashCode` は ID のみに基づく（またはユニーク業務キー）。双方向関連は除外。
- 監査: Hibernate の `@CreationTimestamp`, `@UpdateTimestamp` を使用。
- 取得戦略: `@ManyToOne(fetch = LAZY)`、`@OneToMany` 側もデフォルト LAZY。
- 集約境界: `BeerOrder` を集約ルートとし、`BeerOrderLine` は `BeerOrder` 配下で `cascade = ALL`, `orphanRemoval = true`。
- トランザクション境界: Service 層でメソッド単位に付与（読み取り/更新を分ける）。
- Web 層と永続層は分離（Entity を直接返さず DTO を返す）。

---

### 1) ステータス Enum の定義
```java
package com.example.beer.domain;

public enum OrderStatus { NEW, VALIDATION_PENDING, VALIDATED, ALLOCATED, PARTIALLY_ALLOCATED, PICKED_UP, DELIVERED, CANCELED }

public enum LineStatus { NEW, ALLOCATED, BACKORDER, CANCELED }
```

`Beer` の状態が必要なら別 Enum を追加してください。

---

### 2) エンティティ定義（Lombok + JPA）

#### Beer
```java
package com.example.beer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "beer")
public class Beer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    @Column(nullable = false, length = 100)
    private String beerName;

    @Column(nullable = false, length = 40)
    private String beerStyle;

    @Column(nullable = false, unique = true, length = 30)
    private String upc;

    private Integer quantityOnHand;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
```

- 集約主語は `BeerOrder` なので、`Beer` 側に `@OneToMany` は持たせない（循環の複雑さを避けるため基本は単方向参照）。必要なら読み取り用に双方向を追加可能。

#### BeerOrder
```java
package com.example.beer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "beer_order")
public class BeerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    @Column(length = 64)
    private String customerRef;

    @Column(precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.NEW;

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

    // BeerOrder 1 — * BeerOrderLine（所有者は子：BeerOrderLine.beerOrder）
    @OneToMany(
        mappedBy = "beerOrder",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<BeerOrderLine> lines = new ArrayList<>();

    // 双方向関連の整合性を保つヘルパー
    public void addLine(BeerOrderLine line) {
        lines.add(line);
        line.setBeerOrder(this);
    }

    public void removeLine(BeerOrderLine line) {
        lines.remove(line);
        line.setBeerOrder(null);
    }
}
```

#### BeerOrderLine
```java
package com.example.beer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "beer_order_line")
public class BeerOrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    // 親への参照（所有者）
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beer_order_id", nullable = false)
    private BeerOrder beerOrder;

    // 商品参照
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beer_id", nullable = false)
    private Beer beer;

    private Integer orderQuantity;        // 発注数量
    private Integer quantityAllocated;    // 引当数量

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private LineStatus status = LineStatus.NEW;

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
```

---

### 3) Lombok と JPA の相性に関する注意点
- `@Data` は使わない（`toString` が LAZY 関連を強制初期化しやすく、双方向でスタックオーバーフローの危険）。
- `@EqualsAndHashCode` を使う場合は `id` のみ含める。ID 未採番の一時エンティティ比較は避ける。
- `@Builder` 使用時は JPA のために `@NoArgsConstructor(PROTECTED)` を必ず残す。
- `@ToString(exclude = {"beerOrder", "beer"})` のように双方向関連は除外してもよい。

---

### 4) リポジトリ
```java
package com.example.beer.repositories;

import com.example.beer.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface BeerRepository extends JpaRepository<Beer, Integer> {}
interface BeerOrderRepository extends JpaRepository<BeerOrder, Integer> {}
interface BeerOrderLineRepository extends JpaRepository<BeerOrderLine, Integer> {}
```
- Spring コンポーネントは可能な限り package-private（デフォルト可視性）で十分。

---

### 5) サービス層（トランザクション境界）
```java
package com.example.beer.service;

import com.example.beer.domain.*;
import com.example.beer.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class BeerOrderService {
    private final BeerRepository beerRepository;
    private final BeerOrderRepository orderRepository;

    @Transactional
    public Integer createOrder(Integer beerId, int qty) {
        Beer beer = beerRepository.findById(beerId)
                .orElseThrow();

        BeerOrder order = BeerOrder.builder().status(OrderStatus.NEW).build();
        BeerOrderLine line = BeerOrderLine.builder()
                .beer(beer)
                .orderQuantity(qty)
                .build();

        order.addLine(line); // 双方向整合
        orderRepository.save(order);
        return order.getId();
    }

    @Transactional
    public void allocate(Integer orderId, List<Integer> allocations) {
        BeerOrder order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.ALLOCATED);
        // …行ごとの数量更新ロジック
    }
}
```
- 読み取り専用なら `@Transactional(Transactional.TxType.SUPPORTS)` または Spring の `@Transactional(readOnly = true)` を使用。

---

### 6) マイグレーション/スキーマ例（DDL イメージ）
```sql
create table beer (
  id int primary key auto_increment,
  version int,
  beer_name varchar(100) not null,
  beer_style varchar(40) not null,
  upc varchar(30) not null unique,
  quantity_on_hand int,
  price decimal(19,2) not null,
  created_date timestamp,
  update_date timestamp
);

create table beer_order (
  id int primary key auto_increment,
  version int,
  customer_ref varchar(64),
  payment_amount decimal(19,2),
  status varchar(40) not null,
  created_date timestamp,
  update_date timestamp
);

create table beer_order_line (
  id int primary key auto_increment,
  version int,
  beer_order_id int not null,
  beer_id int not null,
  order_quantity int,
  quantity_allocated int,
  status varchar(30) not null,
  created_date timestamp,
  update_date timestamp,
  constraint fk_line_order foreign key (beer_order_id) references beer_order(id),
  constraint fk_line_beer foreign key (beer_id) references beer(id)
);
```

---

### 7) DTO とマッピング（Web 層分離）
- Controller では Entity を直接返却せず DTO を定義。
- MapStruct 例:
```java
@Mapper(componentModel = "spring")
interface BeerOrderMapper {
    BeerOrderDto toDto(BeerOrder entity);
}
```

---

### 8) 動作確認のための最小フロー
1. `Beer` を 1 件登録。
2. `BeerOrderService#createOrder(beerId, qty)` を呼ぶ。
3. `BeerOrderRepository.findById(id)` で `lines` が取得できるか確認（`@Transactional` 範囲内でアクセス）。

---

### 9) 追加のベストプラクティス
- OSIV 無効化: `spring.jpa.open-in-view=false`
- JSON では DTO のプロパティ命名を camelCase に統一。
- ロギングは SLF4J を使用し `System.out.println` は使わない。
- Testcontainers を使って DB 統合テストを実施し、`@SpringBootTest(webEnvironment = RANDOM_PORT)` でポート競合を回避。

---

### まとめ
- ERD に基づき、`BeerOrder`（親）—`BeerOrderLine`（子）—`Beer`（参照）を JPA でモデリングしました。
- すべての関連は LAZY、集約の整合ヘルパーを提供、Lombok は `@Getter/@Setter/@Builder` を中心に安全な範囲で利用。
- トランザクション境界、DTO 分離、OSIV 無効化など Spring Boot ガイドラインに沿って実装できる構成です。