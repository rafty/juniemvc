package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.repositories.BeerRepository;
import guru.springframework.juniemvc.services.impl.BeerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BeerServiceTest {

    BeerRepository beerRepository;
    BeerService beerService;

    private Beer sampleBeer(Integer id) {
        return Beer.builder()
                .id(id)
                .beerName("Test Lager")
                .beerStyle("LAGER")
                .upc("12345")
                .quantityOnHand(50)
                .price(new BigDecimal("9.99"))
                .build();
    }

    @BeforeEach
    void setUp() {
        beerRepository = Mockito.mock(BeerRepository.class);
        beerService = new BeerServiceImpl(beerRepository);
    }

    @Test
    @DisplayName("create() should delegate to repository.save")
    void create() {
        Beer toSave = sampleBeer(1);
        when(beerRepository.save(any(Beer.class))).thenReturn(toSave);

        Beer saved = beerService.create(toSave);

        assertThat(saved).isEqualTo(toSave);
        verify(beerRepository, times(1)).save(any(Beer.class));
    }

    @Test
    @DisplayName("getById() should return Optional with entity when found")
    void getByIdFound() {
        when(beerRepository.findById(eq(1))).thenReturn(Optional.of(sampleBeer(1)));

        Optional<Beer> res = beerService.getById(1);

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getById() should return empty Optional when not found")
    void getByIdNotFound() {
        when(beerRepository.findById(eq(999))).thenReturn(Optional.empty());

        Optional<Beer> res = beerService.getById(999);

        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("listAll() should return repository.findAll result")
    void listAll() {
        List<Beer> list = Arrays.asList(sampleBeer(1), sampleBeer(2));
        when(beerRepository.findAll()).thenReturn(list);

        List<Beer> result = beerService.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("update() should save entity when found and keep path ID")
    void updateFound() {
        Beer existing = sampleBeer(5);
        Beer payload = sampleBeer(999); // different id in payload should be overridden
        when(beerRepository.findById(eq(5))).thenReturn(Optional.of(existing));
        when(beerRepository.save(any(Beer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Beer> updated = beerService.update(5, payload);

        assertThat(updated).isPresent();
        assertThat(updated.get().getId()).isEqualTo(5);

        ArgumentCaptor<Beer> captor = ArgumentCaptor.forClass(Beer.class);
        verify(beerRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5);
    }

    @Test
    @DisplayName("update() should return empty when entity not found")
    void updateNotFound() {
        Beer payload = sampleBeer(1);
        when(beerRepository.findById(eq(42))).thenReturn(Optional.empty());

        Optional<Beer> updated = beerService.update(42, payload);

        assertThat(updated).isEmpty();
        verify(beerRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete() should delete and return true when existing")
    void deleteFound() {
        when(beerRepository.existsById(eq(7))).thenReturn(true);
        doNothing().when(beerRepository).deleteById(eq(7));

        boolean result = beerService.delete(7);

        assertThat(result).isTrue();
        verify(beerRepository).deleteById(7);
    }

    @Test
    @DisplayName("delete() should return false when not existing")
    void deleteNotFound() {
        when(beerRepository.existsById(eq(8))).thenReturn(false);

        boolean result = beerService.delete(8);

        assertThat(result).isFalse();
        verify(beerRepository, never()).deleteById(any());
    }
}
