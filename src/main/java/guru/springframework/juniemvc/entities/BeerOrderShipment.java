package guru.springframework.juniemvc.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "beer_order_shipment")
public class BeerOrderShipment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beer_order_id", nullable = false)
    private BeerOrder beerOrder;

    @Column(name = "shipment_date", nullable = false)
    private LocalDate shipmentDate;

    @Column(length = 100)
    private String carrier;

    @Column(name = "tracking_number", length = 120)
    private String trackingNumber;
}
