package guru.springframework.juniemvc.repositories;

import guru.springframework.juniemvc.entities.Beer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest(excludeAutoConfiguration = {org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
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

    @Test
    @DisplayName("findAll(Pageable) returns a page of beers")
    void findAllPaged() {
        // seed some data
        for (int i = 0; i < 5; i++) {
            beerRepository.save(Beer.builder()
                    .beerName("Beer " + i)
                    .beerStyle("STYLE")
                    .upc("UPC-" + i)
                    .quantityOnHand(10 + i)
                    .price(new BigDecimal("3.50"))
                    .build());
        }
        beerRepository.flush();

        Page<Beer> page = beerRepository.findAll(PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("findByBeerNameContainingIgnoreCase(filter, Pageable) returns filtered page")
    void findByBeerNameFilteredPaged() {
        beerRepository.saveAndFlush(Beer.builder()
                .beerName("Galaxy Cat IPA")
                .beerStyle("IPA")
                .upc("11111")
                .quantityOnHand(10)
                .price(new BigDecimal("5.00"))
                .build());
        beerRepository.saveAndFlush(Beer.builder()
                .beerName("Porter House")
                .beerStyle("STOUT")
                .upc("22222")
                .quantityOnHand(8)
                .price(new BigDecimal("6.00"))
                .build());

        Page<Beer> page = beerRepository.findByBeerNameContainingIgnoreCase("porter", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getBeerName()).containsIgnoringCase("porter");
    }
}
