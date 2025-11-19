package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.models.BeerDto;
import guru.springframework.juniemvc.models.BeerPatchDto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BeerService {

    BeerDto create(BeerDto beerDto);

    Optional<BeerDto> getById(Integer id);

    Page<BeerDto> list(Pageable pageable, String beerName, String beerStyle);

    Optional<BeerDto> update(Integer id, BeerDto beerDto);

    Optional<BeerDto> patch(Integer id, BeerPatchDto beerPatchDto);

    boolean delete(Integer id);
}
