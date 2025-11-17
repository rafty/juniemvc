package guru.springframework.juniemvc.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer")
public class Customer extends BaseEntity {

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 40)
    private String phoneNumber;

    @Column(length = 255, nullable = false)
    private String addressLine1;

    @Column(length = 255)
    private String addressLine2;

    @Column(length = 100, nullable = false)
    private String city;

    @Column(length = 100, nullable = false)
    private String state;

    @Column(length = 20, nullable = false)
    private String postalCode;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BeerOrder> orders = new ArrayList<>();
}
