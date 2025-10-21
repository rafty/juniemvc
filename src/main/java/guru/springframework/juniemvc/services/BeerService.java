package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.entities.Beer;

import java.util.List;
import java.util.Optional;

public interface BeerService {

    Beer create(Beer beer);

    Optional<Beer> getById(Integer id);

    List<Beer> listAll();
}
