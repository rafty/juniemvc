package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.models.BeerDto;

import java.util.List;
import java.util.Optional;

public interface BeerService {

    BeerDto create(BeerDto beerDto);

    Optional<BeerDto> getById(Integer id);

    List<BeerDto> listAll();

    Optional<BeerDto> update(Integer id, BeerDto beerDto);

    boolean delete(Integer id);
}
