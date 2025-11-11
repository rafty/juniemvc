package guru.springframework.juniemvc.repositories;

import guru.springframework.juniemvc.entities.Beer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BeerRepositoryTest {

    @Autowired
    BeerRepository beerRepository;

    private Beer buildSampleBeer() {
        return Beer.builder()
                .beerName("Test Lager")
                .beerStyle("LAGER")
                .upc("12345")
                .quantityOnHand(50)
                .price(new BigDecimal("9.99"))
                .build();
    }

    @Test
    @DisplayName("Create and Read Beer")
    void createAndRead() {
        Beer saved = beerRepository.saveAndFlush(buildSampleBeer());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getUpdatedDate()).isNotNull();

        Optional<Beer> fetched = beerRepository.findById(saved.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getBeerName()).isEqualTo("Test Lager");
    }

    @Test
    @DisplayName("Update Beer and check version increment")
    void updateBeer() {
        Beer saved = beerRepository.saveAndFlush(buildSampleBeer());
        Integer initialVersion = saved.getVersion();

        saved.setBeerName("Updated Lager");
        Beer updated = beerRepository.saveAndFlush(saved);

        assertThat(updated.getBeerName()).isEqualTo("Updated Lager");
        // Version should be incremented on update (may be null->0 on first save depending on provider)
        if (initialVersion == null) {
            assertThat(updated.getVersion()).isNotNull();
        } else {
            assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        }
    }

    @Test
    @DisplayName("Delete Beer")
    void deleteBeer() {
        Beer saved = beerRepository.saveAndFlush(buildSampleBeer());
        Integer savedId = saved.getId();

        assertThat(beerRepository.findById(savedId)).isPresent();

        beerRepository.deleteById(savedId);

        assertThat(beerRepository.findById(savedId)).isEmpty();
    }
}
