package guru.springframework.juniemvc.repositories;

import guru.springframework.juniemvc.entities.Beer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeerRepository extends JpaRepository<Beer, Integer> {

    Page<Beer> findByBeerNameContainingIgnoreCase(String beerName, Pageable pageable);

    Page<Beer> findByBeerStyleIgnoreCase(String beerStyle, Pageable pageable);

    Page<Beer> findByBeerNameContainingIgnoreCaseAndBeerStyleIgnoreCase(String beerName, String beerStyle, Pageable pageable);
}
