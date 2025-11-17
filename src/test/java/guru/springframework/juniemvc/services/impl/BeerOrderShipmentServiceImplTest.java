package guru.springframework.juniemvc.services.impl;

import guru.springframework.juniemvc.entities.BeerOrder;
import guru.springframework.juniemvc.entities.BeerOrderShipment;
import guru.springframework.juniemvc.mappers.BeerOrderShipmentMapper;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentRequest;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentResponse;
import guru.springframework.juniemvc.repositories.BeerOrderRepository;
import guru.springframework.juniemvc.repositories.BeerOrderShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BeerOrderShipmentServiceImplTest {

    BeerOrderRepository beerOrderRepository;
    BeerOrderShipmentRepository beerOrderShipmentRepository;
    BeerOrderShipmentMapper mapper;

    BeerOrderShipmentServiceImpl service;

    @BeforeEach
    void setUp() {
        beerOrderRepository = mock(BeerOrderRepository.class);
        beerOrderShipmentRepository = mock(BeerOrderShipmentRepository.class);
        mapper = mock(BeerOrderShipmentMapper.class);
        service = new BeerOrderShipmentServiceImpl(beerOrderRepository, beerOrderShipmentRepository, mapper);
    }

    @Test
    void create_savesEntityAndReturnsResponse() {
        Integer orderId = 1;
        BeerOrder parent = BeerOrder.builder().id(orderId).build();
        when(beerOrderRepository.findById(orderId)).thenReturn(Optional.of(parent));

        BeerOrderShipmentRequest req = new BeerOrderShipmentRequest(LocalDate.now(), "UPS", "TN");
        BeerOrderShipment entity = new BeerOrderShipment();
        when(mapper.toEntity(req)).thenReturn(entity);

        BeerOrderShipment saved = new BeerOrderShipment();
        when(beerOrderShipmentRepository.save(entity)).thenReturn(saved);

        BeerOrderShipmentResponse resp = new BeerOrderShipmentResponse(10, req.shipmentDate(), req.carrier(), req.trackingNumber());
        when(mapper.toResponse(saved)).thenReturn(resp);

        BeerOrderShipmentResponse result = service.create(orderId, req);

        assertThat(result.id()).isEqualTo(10);
        ArgumentCaptor<BeerOrderShipment> captor = ArgumentCaptor.forClass(BeerOrderShipment.class);
        verify(beerOrderShipmentRepository).save(captor.capture());
        assertThat(captor.getValue().getBeerOrder()).isSameAs(parent);
    }

    @Test
    void getById_returnsResponse() {
        Integer orderId = 1;
        Integer shipmentId = 2;
        BeerOrderShipment entity = new BeerOrderShipment();
        when(beerOrderShipmentRepository.findByIdAndBeerOrderId(shipmentId, orderId)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new BeerOrderShipmentResponse(shipmentId, LocalDate.now(), "UPS", "TN"));

        BeerOrderShipmentResponse result = service.getById(orderId, shipmentId);
        assertThat(result.id()).isEqualTo(2);
    }

    @Test
    void update_updatesAndSaves() {
        Integer orderId = 1;
        Integer shipmentId = 2;
        BeerOrderShipment entity = new BeerOrderShipment();
        when(beerOrderShipmentRepository.findByIdAndBeerOrderId(shipmentId, orderId)).thenReturn(Optional.of(entity));
        doAnswer(invocation -> {
            BeerOrderShipment target = invocation.getArgument(0);
            BeerOrderShipmentRequest src = invocation.getArgument(1);
            target.setCarrier(src.carrier());
            return null;
        }).when(mapper).update(any(BeerOrderShipment.class), any(BeerOrderShipmentRequest.class));

        when(beerOrderShipmentRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(new BeerOrderShipmentResponse(shipmentId, LocalDate.now(), "FedEx", "TN"));

        BeerOrderShipmentResponse result = service.update(orderId, shipmentId, new BeerOrderShipmentRequest(LocalDate.now(), "FedEx", "TN"));
        assertThat(result.carrier()).isEqualTo("FedEx");
    }

    @Test
    void delete_deletesEntity() {
        Integer orderId = 1;
        Integer shipmentId = 2;
        BeerOrderShipment entity = new BeerOrderShipment();
        when(beerOrderShipmentRepository.findByIdAndBeerOrderId(shipmentId, orderId)).thenReturn(Optional.of(entity));

        service.delete(orderId, shipmentId);

        verify(beerOrderShipmentRepository).delete(entity);
    }
}
