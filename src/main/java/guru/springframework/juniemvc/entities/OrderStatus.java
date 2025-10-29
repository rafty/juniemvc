package guru.springframework.juniemvc.entities;

// enum for order status
public enum OrderStatus {
    NEW,
    VALIDATION_PENDING,
    VALIDATED,
    ALLOCATED,
    PARTIALLY_ALLOCATED,
    PICKED_UP,
    DELIVERED,
    CANCELED
}
