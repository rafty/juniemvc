package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.mappers.BeerMapper;
import guru.springframework.juniemvc.models.BeerDto;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BeerServiceTest {

    BeerRepository beerRepository;
    BeerMapper beerMapper;
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

    private BeerDto sampleDto(Integer id) {
        return BeerDto.builder()
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
        beerMapper = Mockito.mock(BeerMapper.class);
        beerService = new BeerServiceImpl(beerRepository, beerMapper);
    }

    @Test
    @DisplayName("create() should map DTO->Entity, save, and return DTO")
    void create() {
        BeerDto payload = sampleDto(null);
        Beer entityToSave = sampleBeer(null);
        Beer savedEntity = sampleBeer(1);
        BeerDto savedDto = sampleDto(1);

        when(beerMapper.toEntity(any(BeerDto.class))).thenReturn(entityToSave);
        when(beerRepository.save(any(Beer.class))).thenReturn(savedEntity);
        when(beerMapper.toDto(any(Beer.class))).thenReturn(savedDto);

        BeerDto result = beerService.create(payload);

        assertThat(result.getId()).isEqualTo(1);
        verify(beerMapper).toEntity(any(BeerDto.class));
        verify(beerRepository).save(any(Beer.class));
        verify(beerMapper).toDto(any(Beer.class));
    }

    @Test
    @DisplayName("getById() should return Optional DTO when found")
    void getByIdFound() {
        when(beerRepository.findById(eq(1))).thenReturn(Optional.of(sampleBeer(1)));
        when(beerMapper.toDto(any(Beer.class))).thenReturn(sampleDto(1));

        Optional<BeerDto> res = beerService.getById(1);

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getById() should return empty Optional when not found")
    void getByIdNotFound() {
        when(beerRepository.findById(eq(999))).thenReturn(Optional.empty());

        Optional<BeerDto> res = beerService.getById(999);

        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("list(pageable, null) should use findAll(pageable) and map page of DTOs")
    void listPagedNoFilter() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 2);
        List<Beer> entities = Arrays.asList(sampleBeer(1), sampleBeer(2));
        org.springframework.data.domain.Page<Beer> entityPage = new org.springframework.data.domain.PageImpl<>(entities, pageable, 2);

        when(beerRepository.findAll(eq(pageable))).thenReturn(entityPage);
        when(beerMapper.toDto(any(Beer.class)))
                .thenReturn(sampleDto(1))
                .thenReturn(sampleDto(2));

        org.springframework.data.domain.Page<BeerDto> result = beerService.list(pageable, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1);
        verify(beerRepository, times(1)).findAll(eq(pageable));
        verify(beerRepository, never()).findByBeerNameContainingIgnoreCase(anyString(), any());
        verify(beerRepository, never()).findByBeerStyleIgnoreCase(anyString(), any());
        verify(beerRepository, never()).findByBeerNameContainingIgnoreCaseAndBeerStyleIgnoreCase(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("list(pageable, beerName) should use filter method and map page of DTOs")
    void listPagedWithFilter() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        List<Beer> entities = Arrays.asList(sampleBeer(1));
        org.springframework.data.domain.Page<Beer> entityPage = new org.springframework.data.domain.PageImpl<>(entities, pageable, 1);

        when(beerRepository.findByBeerNameContainingIgnoreCase(eq("Lager"), eq(pageable))).thenReturn(entityPage);
        when(beerMapper.toDto(any(Beer.class))).thenReturn(sampleDto(1));

        org.springframework.data.domain.Page<BeerDto> result = beerService.list(pageable, "Lager", null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getBeerName()).isEqualTo("Test Lager");
        verify(beerRepository, times(1)).findByBeerNameContainingIgnoreCase(eq("Lager"), eq(pageable));
        verify(beerRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
        verify(beerRepository, never()).findByBeerStyleIgnoreCase(anyString(), any());
        verify(beerRepository, never()).findByBeerNameContainingIgnoreCaseAndBeerStyleIgnoreCase(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("update() should update existing entity via mapper and return DTO")
    void updateFound() {
        Beer existing = sampleBeer(5);
        BeerDto payload = sampleDto(null);
        Beer saved = sampleBeer(5);
        BeerDto savedDto = sampleDto(5);

        when(beerRepository.findById(eq(5))).thenReturn(Optional.of(existing));
        doAnswer(invocation -> null).when(beerMapper).updateEntity(any(Beer.class), any(BeerDto.class));
        when(beerRepository.save(any(Beer.class))).thenReturn(saved);
        when(beerMapper.toDto(any(Beer.class))).thenReturn(savedDto);

        Optional<BeerDto> updated = beerService.update(5, payload);

        assertThat(updated).isPresent();
        assertThat(updated.get().getId()).isEqualTo(5);

        ArgumentCaptor<Beer> captor = ArgumentCaptor.forClass(Beer.class);
        verify(beerRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5);
    }

    @Test
    @DisplayName("update() should return empty when entity not found")
    void updateNotFound() {
        BeerDto payload = sampleDto(1);
        when(beerRepository.findById(eq(42))).thenReturn(Optional.empty());

        Optional<BeerDto> updated = beerService.update(42, payload);

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

    @Test
    @DisplayName("list(pageable, null, style) should use style filter only")
    void listPagedWithStyleOnly() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 3);
        java.util.List<Beer> entities = java.util.Arrays.asList(sampleBeer(3));
        org.springframework.data.domain.Page<Beer> entityPage = new org.springframework.data.domain.PageImpl<>(entities, pageable, 1);

        when(beerRepository.findByBeerStyleIgnoreCase(eq("IPA"), eq(pageable))).thenReturn(entityPage);
        when(beerMapper.toDto(any(Beer.class))).thenReturn(sampleDto(3));

        org.springframework.data.domain.Page<BeerDto> result = beerService.list(pageable, null, "IPA");

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(beerRepository, times(1)).findByBeerStyleIgnoreCase(eq("IPA"), eq(pageable));
        verify(beerRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
        verify(beerRepository, never()).findByBeerNameContainingIgnoreCase(anyString(), any());
        verify(beerRepository, never()).findByBeerNameContainingIgnoreCaseAndBeerStyleIgnoreCase(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("list(pageable, name, style) should use combined filter")
    void listPagedWithBothFilters() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(1, 5);
        java.util.List<Beer> entities = java.util.Arrays.asList(sampleBeer(4));
        org.springframework.data.domain.Page<Beer> entityPage = new org.springframework.data.domain.PageImpl<>(entities, pageable, 1);

        when(beerRepository.findByBeerNameContainingIgnoreCaseAndBeerStyleIgnoreCase(eq("Cat"), eq("LAGER"), eq(pageable))).thenReturn(entityPage);
        when(beerMapper.toDto(any(Beer.class))).thenReturn(sampleDto(4));

        org.springframework.data.domain.Page<BeerDto> result = beerService.list(pageable, "Cat", "LAGER");

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(beerRepository, times(1)).findByBeerNameContainingIgnoreCaseAndBeerStyleIgnoreCase(eq("Cat"), eq("LAGER"), eq(pageable));
        verify(beerRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
        verify(beerRepository, never()).findByBeerNameContainingIgnoreCase(anyString(), any());
        verify(beerRepository, never()).findByBeerStyleIgnoreCase(anyString(), any());
    }
}
