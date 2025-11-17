package guru.springframework.juniemvc.mappers;

import guru.springframework.juniemvc.entities.BeerOrder;
import guru.springframework.juniemvc.entities.BeerOrderShipment;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentRequest;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BeerOrderShipmentMapperTest {

    private final BeerOrderShipmentMapper mapper = Mappers.getMapper(BeerOrderShipmentMapper.class);

    @Test
    void toEntity_mapsFields() {
        BeerOrderShipmentRequest req = new BeerOrderShipmentRequest(LocalDate.of(2025, 1, 2), "UPS", "1Z999");
        BeerOrderShipment entity = mapper.toEntity(req);
        assertThat(entity.getShipmentDate()).isEqualTo(LocalDate.of(2025, 1, 2));
        assertThat(entity.getCarrier()).isEqualTo("UPS");
        assertThat(entity.getTrackingNumber()).isEqualTo("1Z999");
    }

    @Test
    void toResponse_mapsFields() {
        BeerOrderShipment entity = new BeerOrderShipment();
        entity.setShipmentDate(LocalDate.of(2025, 2, 3));
        entity.setCarrier("FedEx");
        entity.setTrackingNumber("TRK-123");
        entity.setBeerOrder(new BeerOrder());
        BeerOrderShipmentResponse res = mapper.toResponse(entity);
        assertThat(res.shipmentDate()).isEqualTo(LocalDate.of(2025, 2, 3));
        assertThat(res.carrier()).isEqualTo("FedEx");
        assertThat(res.trackingNumber()).isEqualTo("TRK-123");
    }

    @Test
    void update_updatesFields() {
        BeerOrderShipment target = new BeerOrderShipment();
        target.setShipmentDate(LocalDate.of(2024, 12, 31));
        target.setCarrier("DHL");
        target.setTrackingNumber("OLD");

        BeerOrderShipmentRequest req = new BeerOrderShipmentRequest(LocalDate.of(2025, 3, 4), "Sagawa", "NEW");
        mapper.update(target, req);

        assertThat(target.getShipmentDate()).isEqualTo(LocalDate.of(2025, 3, 4));
        assertThat(target.getCarrier()).isEqualTo("Sagawa");
        assertThat(target.getTrackingNumber()).isEqualTo("NEW");
    }
}
