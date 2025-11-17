package guru.springframework.juniemvc.repositories;

import guru.springframework.juniemvc.entities.BeerOrderShipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BeerOrderShipmentRepository extends JpaRepository<BeerOrderShipment, Integer> {

    Page<BeerOrderShipment> findAllByBeerOrderId(Integer beerOrderId, Pageable pageable);

    Optional<BeerOrderShipment> findByIdAndBeerOrderId(Integer id, Integer beerOrderId);
}
