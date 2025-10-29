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
    private Integer id;
    private Integer version;

    @NotBlank(message = "beerName must not be blank")
    private String beerName;

    @NotBlank(message = "beerStyle must not be blank")
    private String beerStyle;

    @NotBlank(message = "upc must not be blank")
    private String upc;

    @PositiveOrZero
    private Integer quantityOnHand;

    @Positive
    private BigDecimal price;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
