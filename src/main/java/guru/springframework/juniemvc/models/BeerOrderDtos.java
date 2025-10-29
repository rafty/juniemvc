package guru.springframework.juniemvc.models;

import guru.springframework.juniemvc.entities.LineStatus;
import guru.springframework.juniemvc.entities.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Request/Response DTOs for BeerOrder use cases
public final class BeerOrderDtos {

    private BeerOrderDtos() {}

    // Create Request
    public record BeerOrderCreateRequest(
            @Size(max = 64)
            String customerRef,
            @Digits(integer = 17, fraction = 2)
            @PositiveOrZero
            BigDecimal paymentAmount,
            @NotEmpty
            List<@Valid BeerOrderLineCreateItem> lines
    ) {}

    public record BeerOrderLineCreateItem(
            @NotNull @Positive Integer beerId,
            @NotNull @Positive Integer orderQuantity
    ) {}

    // Response
    public record BeerOrderResponse(
            Integer id,
            Integer version,
            String customerRef,
            BigDecimal paymentAmount,
            OrderStatus status,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            List<BeerOrderLineResponse> lines
    ) {}

    public record BeerOrderLineResponse(
            Integer beerId,
            Integer orderQuantity,
            Integer quantityAllocated,
            LineStatus status
    ) {}
}
