package guru.springframework.juniemvc.models;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class BeerOrderShipmentDtos {

    private BeerOrderShipmentDtos() {}

    public record BeerOrderShipmentRequest(
            @NotNull LocalDate shipmentDate,
            String carrier,
            String trackingNumber
    ) {}

    public record BeerOrderShipmentResponse(
            Integer id,
            LocalDate shipmentDate,
            String carrier,
            String trackingNumber
    ) {}
}
