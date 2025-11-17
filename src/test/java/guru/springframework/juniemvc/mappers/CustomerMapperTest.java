package guru.springframework.juniemvc.mappers;

import guru.springframework.juniemvc.entities.Customer;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerCreateRequest;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerResponse;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerUpdateRequest;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private final CustomerMapper mapper = Mappers.getMapper(CustomerMapper.class);

    @Test
    void toEntity_mapsAllFields() {
        CustomerCreateRequest req = new CustomerCreateRequest(
                "John Doe",
                "john@example.com",
                "+1-234",
                "123 Main St",
                null,
                "Springfield",
                "IL",
                "62704"
        );

        Customer entity = mapper.toEntity(req);
        assertThat(entity.getName()).isEqualTo("John Doe");
        assertThat(entity.getEmail()).isEqualTo("john@example.com");
        assertThat(entity.getPhoneNumber()).isEqualTo("+1-234");
        assertThat(entity.getAddressLine1()).isEqualTo("123 Main St");
        assertThat(entity.getAddressLine2()).isNull();
        assertThat(entity.getCity()).isEqualTo("Springfield");
        assertThat(entity.getState()).isEqualTo("IL");
        assertThat(entity.getPostalCode()).isEqualTo("62704");
    }

    @Test
    void updateEntity_setsValuesAndAllowsNulls() {
        Customer existing = Customer.builder()
                .name("Old Name")
                .email("old@example.com")
                .phoneNumber("000")
                .addressLine1("old-addr1")
                .addressLine2("old-addr2")
                .city("Old City")
                .state("OS")
                .postalCode("00000")
                .build();

        CustomerUpdateRequest req = new CustomerUpdateRequest(
                "New Name",
                null, // email null should overwrite to null per SET_TO_NULL
                "111",
                "new-addr1",
                null, // addressLine2 null
                "New City",
                "NS",
                "99999"
        );

        mapper.updateEntity(existing, req);
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getEmail()).isNull();
        assertThat(existing.getPhoneNumber()).isEqualTo("111");
        assertThat(existing.getAddressLine1()).isEqualTo("new-addr1");
        assertThat(existing.getAddressLine2()).isNull();
        assertThat(existing.getCity()).isEqualTo("New City");
        assertThat(existing.getState()).isEqualTo("NS");
        assertThat(existing.getPostalCode()).isEqualTo("99999");
    }

    @Test
    void toResponse_mapsAllFields() {
        Customer entity = Customer.builder()
                .name("Jane")
                .email("jane@example.com")
                .phoneNumber("222")
                .addressLine1("A1")
                .addressLine2("A2")
                .city("Metropolis")
                .state("CA")
                .postalCode("12345")
                .build();
        entity.setId(42);
        entity.setVersion(7);

        CustomerResponse resp = mapper.toResponse(entity);
        assertThat(resp.id()).isEqualTo(42);
        assertThat(resp.version()).isEqualTo(7);
        assertThat(resp.name()).isEqualTo("Jane");
        assertThat(resp.email()).isEqualTo("jane@example.com");
        assertThat(resp.phoneNumber()).isEqualTo("222");
        assertThat(resp.addressLine1()).isEqualTo("A1");
        assertThat(resp.addressLine2()).isEqualTo("A2");
        assertThat(resp.city()).isEqualTo("Metropolis");
        assertThat(resp.state()).isEqualTo("CA");
        assertThat(resp.postalCode()).isEqualTo("12345");
    }
}
