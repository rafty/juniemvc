package guru.springframework.juniemvc.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA Entity representing a Beer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "beer")
public class Beer {

    @Id
    private Integer id;

    @Version
    private Integer version;

    private String beerName;

    private String beerStyle;

    private String upc;

    private Integer quantityOnHand;

    private BigDecimal price;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;
}
