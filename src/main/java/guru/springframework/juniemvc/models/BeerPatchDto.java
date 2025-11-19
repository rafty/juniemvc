package guru.springframework.juniemvc.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Patch DTO for Beer partial updates. All fields are optional and may be null.
 * No validation annotations should be present to allow partial updates.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeerPatchDto {

    private String beerName;

    private String beerStyle;

    private String upc;

    private Integer quantityOnHand;

    private BigDecimal price;

    private String description;
}
