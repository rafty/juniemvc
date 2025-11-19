package guru.springframework.juniemvc.services.impl;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.mappers.BeerMapper;
import guru.springframework.juniemvc.models.BeerDto;
import guru.springframework.juniemvc.repositories.BeerRepository;
import guru.springframework.juniemvc.services.BeerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BeerServiceImpl implements BeerService {

    private final BeerRepository beerRepository;
    private final BeerMapper beerMapper;

    public BeerServiceImpl(BeerRepository beerRepository, BeerMapper beerMapper) {
        this.beerRepository = beerRepository;
        this.beerMapper = beerMapper;
    }

    @Override
    public BeerDto create(BeerDto beerDto) {
        Beer toSave = beerMapper.toEntity(beerDto);
        Beer saved = beerRepository.save(toSave);
        return beerMapper.toDto(saved);
    }

    @Override
    public Optional<BeerDto> getById(Integer id) {
        return beerRepository.findById(id).map(beerMapper::toDto);
    }

    @Override
    public Page<BeerDto> list(Pageable pageable, String beerName, String beerStyle) {
        Page<Beer> page;
        boolean hasName = beerName != null && !beerName.isBlank();
        boolean hasStyle = beerStyle != null && !beerStyle.isBlank();
        if (hasName && hasStyle) {
            page = beerRepository.findByBeerNameContainingIgnoreCaseAndBeerStyleIgnoreCase(beerName, beerStyle, pageable);
        } else if (hasName) {
            page = beerRepository.findByBeerNameContainingIgnoreCase(beerName, pageable);
        } else if (hasStyle) {
            page = beerRepository.findByBeerStyleIgnoreCase(beerStyle, pageable);
        } else {
            page = beerRepository.findAll(pageable);
        }
        return page.map(beerMapper::toDto);
    }

    @Override
    public Optional<BeerDto> update(Integer id, BeerDto beerDto) {
        return beerRepository.findById(id).map(existing -> {
            // keep id/createdDate/updatedDate from existing entity
            beerMapper.updateEntity(existing, beerDto);
            existing.setId(id);
            Beer saved = beerRepository.save(existing);
            return beerMapper.toDto(saved);
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
