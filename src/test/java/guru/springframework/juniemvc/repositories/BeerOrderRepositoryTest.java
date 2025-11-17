package guru.springframework.juniemvc.repositories;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.entities.BeerOrder;
import guru.springframework.juniemvc.entities.BeerOrderLine;
import guru.springframework.juniemvc.entities.LineStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest(excludeAutoConfiguration = {org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BeerOrderRepositoryTest {

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Test
    @DisplayName("save parent+child; verify cascade, version, timestamps")
    void saveParentChild() {
        // given beers
        Beer beer1 = Beer.builder().beerName("IPA").beerStyle("IPA").upc("UP1").price(new BigDecimal("3.50")).build();
        Beer beer2 = Beer.builder().beerName("Lager").beerStyle("LAGER").upc("UP2").price(new BigDecimal("4.00")).build();
        beer1 = beerRepository.save(beer1);
        beer2 = beerRepository.save(beer2);

        BeerOrder order = new BeerOrder();
        order.setCustomerRef("CUST");

        BeerOrderLine l1 = new BeerOrderLine();
        l1.setBeer(beer1);
        l1.setOrderQuantity(2);
        l1.setQuantityAllocated(0);
        l1.setStatus(LineStatus.NEW);

        BeerOrderLine l2 = new BeerOrderLine();
        l2.setBeer(beer2);
        l2.setOrderQuantity(1);
        l2.setQuantityAllocated(0);
        l2.setStatus(LineStatus.NEW);

        order.addLine(l1);
        order.addLine(l2);

        // when
        BeerOrder saved = beerOrderRepository.save(order);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getUpdatedDate()).isNotNull();
        assertThat(saved.getLines()).hasSize(2);
        assertThat(saved.getLines().get(0).getId()).isNotNull();
        assertThat(saved.getLines().get(0).getBeerOrder()).isNotNull();
    }
}
