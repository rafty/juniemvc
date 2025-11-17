package guru.springframework.juniemvc.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class CustomerDtos {

    private CustomerDtos() {}

    public record CustomerCreateRequest(
            @NotBlank @Size(max = 255) String name,
            @Email @Size(max = 255) String email,
            @Size(max = 40) String phoneNumber,
            @NotBlank @Size(max = 255) String addressLine1,
            @Size(max = 255) String addressLine2,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 20) String postalCode
    ) {}

    public record CustomerUpdateRequest(
            @NotBlank @Size(max = 255) String name,
            @Email @Size(max = 255) String email,
            @Size(max = 40) String phoneNumber,
            @NotBlank @Size(max = 255) String addressLine1,
            @Size(max = 255) String addressLine2,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotBlank @Size(max = 20) String postalCode
    ) {}

    public record CustomerResponse(
            Integer id,
            Integer version,
            String name,
            String email,
            String phoneNumber,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            LocalDateTime createdDate,
            LocalDateTime updatedDate
    ) {}
}
