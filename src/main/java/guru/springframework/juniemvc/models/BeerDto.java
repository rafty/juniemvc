package guru.springframework.juniemvc.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object for Beer API (separates Web and Persistence layers).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeerDto {

    // read only properties
    private Integer id;
    private Integer version;

    @NotBlank(message = "beerName must not be blank")
    private String beerName;

    // style of the beer, ALE, PALE ALE, IPA, etc
    @NotBlank(message = "beerStyle must not be blank")
    private String beerStyle;

    // Universal Product Code, a 13-digit number assigned to each unique beer product by the Federal Bar Association
    @NotBlank(message = "upc must not be blank")
    private String upc;

    @PositiveOrZero
    private Integer quantityOnHand;

    @Positive
    private BigDecimal price;

    // Optional human-readable description of the beer
    private String description;

    // read only properties
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
