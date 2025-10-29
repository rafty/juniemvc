package guru.springframework.juniemvc.services.impl;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.entities.BeerOrder;
import guru.springframework.juniemvc.entities.BeerOrderLine;
import guru.springframework.juniemvc.entities.LineStatus;
import guru.springframework.juniemvc.exceptions.BeerNotFoundException;
import guru.springframework.juniemvc.exceptions.InvalidOrderException;
import guru.springframework.juniemvc.mappers.BeerOrderMapper;
import guru.springframework.juniemvc.models.BeerOrderDtos;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderCreateRequest;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderLineCreateItem;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderResponse;
import guru.springframework.juniemvc.repositories.BeerOrderLineRepository;
import guru.springframework.juniemvc.repositories.BeerOrderRepository;
import guru.springframework.juniemvc.repositories.BeerRepository;
import guru.springframework.juniemvc.services.BeerOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BeerOrderServiceTest {

    BeerRepository beerRepository;
    BeerOrderRepository beerOrderRepository;
    BeerOrderLineRepository beerOrderLineRepository;
    BeerOrderMapper beerOrderMapper;

    BeerOrderService service;

    @BeforeEach
    void setUp() {
        beerRepository = Mockito.mock(BeerRepository.class);
        beerOrderRepository = Mockito.mock(BeerOrderRepository.class);
        beerOrderLineRepository = Mockito.mock(BeerOrderLineRepository.class);
        beerOrderMapper = Mockito.mock(BeerOrderMapper.class);
        service = new BeerOrderServiceImpl(beerRepository, beerOrderRepository, beerOrderLineRepository, beerOrderMapper);
    }

    private Beer sampleBeer(Integer id) {
        return Beer.builder().id(id).beerName("IPA").beerStyle("IPA").upc("UPC-1").price(new BigDecimal("5.99")).build();
    }

    @Test
    @DisplayName("create(): success when all beers exist")
    void createSuccess() {
        BeerOrderCreateRequest req = new BeerOrderCreateRequest(
                "CUST-1",
                new BigDecimal("12.34"),
                List.of(new BeerOrderLineCreateItem(1, 2), new BeerOrderLineCreateItem(2, 3))
        );

        when(beerRepository.findById(eq(1))).thenReturn(Optional.of(sampleBeer(1)));
        when(beerRepository.findById(eq(2))).thenReturn(Optional.of(sampleBeer(2)));

        // Return saved order
        BeerOrder saved = new BeerOrder();
        saved.setId(99);
        BeerOrderLine l1 = new BeerOrderLine(); l1.setBeer(sampleBeer(1)); l1.setOrderQuantity(2); l1.setQuantityAllocated(0); l1.setStatus(LineStatus.NEW);
        BeerOrderLine l2 = new BeerOrderLine(); l2.setBeer(sampleBeer(2)); l2.setOrderQuantity(3); l2.setQuantityAllocated(0); l2.setStatus(LineStatus.NEW);
        saved.addLine(l1);
        saved.addLine(l2);

        when(beerOrderRepository.save(any(BeerOrder.class))).thenReturn(saved);

        BeerOrderResponse mapped = new BeerOrderDtos.BeerOrderResponse(99, 0, "CUST-1", new BigDecimal("12.34"), null, null, null, List.of());
        when(beerOrderMapper.toResponse(any(BeerOrder.class))).thenReturn(mapped);

        BeerOrderResponse res = service.create(req);

        assertThat(res.id()).isEqualTo(99);
        verify(beerOrderRepository).save(any(BeerOrder.class));
        verify(beerOrderMapper).toResponse(any(BeerOrder.class));
    }

    @Test
    @DisplayName("create(): throws when any beerId not found")
    void createMissingBeer() {
        BeerOrderCreateRequest req = new BeerOrderCreateRequest(
                null,
                null,
                List.of(new BeerOrderLineCreateItem(1, 1))
        );
        when(beerRepository.findById(eq(1))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BeerNotFoundException.class);
        verify(beerOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("create(): throws InvalidOrderException when lines empty")
    void createInvalid() {
        BeerOrderCreateRequest req = new BeerOrderCreateRequest("CUST", null, List.of());
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("getById(): success")
    void getByIdSuccess() {
        BeerOrder order = new BeerOrder();
        order.setId(10);
        when(beerOrderRepository.findByIdWithLines(eq(10))).thenReturn(Optional.of(order));
        when(beerOrderMapper.toResponse(any(BeerOrder.class)))
                .thenReturn(new BeerOrderResponse(10, 0, null, null, null, null, null, List.of()));

        BeerOrderResponse res = service.getById(10);
        assertThat(res.id()).isEqualTo(10);
        verify(beerOrderRepository).findByIdWithLines(10);
    }

    @Test
    @DisplayName("getById(): throws when not found")
    void getByIdNotFound() {
        when(beerOrderRepository.findByIdWithLines(eq(404))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(404))
                .isInstanceOf(InvalidOrderException.class);
    }
}
