package guru.springframework.juniemvc.repositories;

import guru.springframework.juniemvc.entities.BeerOrder;
import guru.springframework.juniemvc.entities.BeerOrderShipment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BeerOrderShipmentRepositoryTest {

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    BeerOrderShipmentRepository beerOrderShipmentRepository;

    @Test
    void findByIdAndBeerOrderId_and_findAllByBeerOrderId() {
        BeerOrder order = beerOrderRepository.save(BeerOrder.builder().customerRef("C1").build());

        BeerOrderShipment s1 = new BeerOrderShipment();
        s1.setBeerOrder(order);
        s1.setShipmentDate(LocalDate.now());
        s1.setCarrier("UPS");
        s1.setTrackingNumber("T1");
        s1 = beerOrderShipmentRepository.save(s1);

        Page<BeerOrderShipment> page = beerOrderShipmentRepository.findAllByBeerOrderId(order.getId(), PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);

        assertThat(beerOrderShipmentRepository.findByIdAndBeerOrderId(s1.getId(), order.getId())).isPresent();
        assertThat(beerOrderShipmentRepository.findByIdAndBeerOrderId(-999, order.getId())).isNotPresent();
    }
}
