package guru.springframework.juniemvc.services.impl;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.repositories.BeerRepository;
import guru.springframework.juniemvc.services.BeerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BeerServiceImpl implements BeerService {

    private final BeerRepository beerRepository;

    public BeerServiceImpl(BeerRepository beerRepository) {
        this.beerRepository = beerRepository;
    }

    @Override
    public Beer create(Beer beer) {
        return beerRepository.save(beer);
    }

    @Override
    public Optional<Beer> getById(Integer id) {
        return beerRepository.findById(id);
    }

    @Override
    public List<Beer> listAll() {
        return beerRepository.findAll();
    }

    @Override
    public Optional<Beer> update(Integer id, Beer beer) {
        return beerRepository.findById(id).map(existing -> {
            // Update fields; keep ID from path
            beer.setId(id);
            return beerRepository.save(beer);
        });
    }

    @Override
    public boolean delete(Integer id) {
        if (beerRepository.existsById(id)) {
            beerRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
