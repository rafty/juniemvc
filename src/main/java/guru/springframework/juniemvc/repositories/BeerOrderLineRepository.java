package guru.springframework.juniemvc.repositories;

import guru.springframework.juniemvc.entities.BeerOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeerOrderLineRepository extends JpaRepository<BeerOrderLine, Integer> {
}
