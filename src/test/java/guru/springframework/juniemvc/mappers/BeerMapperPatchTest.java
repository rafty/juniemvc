package guru.springframework.juniemvc.mappers;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.models.BeerPatchDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BeerMapperPatchTest {

    private final BeerMapper mapper = Mappers.getMapper(BeerMapper.class);

    @Test
    void updateFromPatch_appliesOnlyNonNulls() {
        Beer target = Beer.builder()
                .id(1)
                .beerName("Old Name")
                .beerStyle("LAGER")
                .upc("111")
                .quantityOnHand(10)
                .price(new BigDecimal("5.00"))
                .description("Old")
                .build();

        BeerPatchDto patch = BeerPatchDto.builder()
                .beerName("New Name")
                .price(new BigDecimal("6.50"))
                .build();

        mapper.updateFromPatch(target, patch);

        assertThat(target.getBeerName()).isEqualTo("New Name");
        assertThat(target.getPrice()).isEqualTo(new BigDecimal("6.50"));
        // unchanged fields remain the same
        assertThat(target.getBeerStyle()).isEqualTo("LAGER");
        assertThat(target.getUpc()).isEqualTo("111");
        assertThat(target.getQuantityOnHand()).isEqualTo(10);
        assertThat(target.getDescription()).isEqualTo("Old");
    }
}
