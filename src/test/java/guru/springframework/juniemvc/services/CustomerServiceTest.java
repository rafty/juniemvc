package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.models.CustomerDtos.CustomerCreateRequest;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerResponse;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CustomerServiceTest {

    @Autowired
    CustomerService service;

    @Test
    void create_get_update_delete_flow() {
        // create
        CustomerCreateRequest create = new CustomerCreateRequest("Alice", "a@example.com", null,
                "A1", null, "City", "ST", "00001");
        CustomerResponse created = service.create(create);
        assertThat(created.id()).isNotNull();
        Integer id = created.id();

        // get
        Optional<CustomerResponse> got = service.getById(id);
        assertThat(got).isPresent();
        assertThat(got.get().name()).isEqualTo("Alice");

        // list
        Page<CustomerResponse> page = service.list(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);

        // update
        CustomerUpdateRequest upd = new CustomerUpdateRequest("Alice2", null, null,
                "A1-2", null, "City2", "ST", "00002");
        Optional<CustomerResponse> updated = service.update(id, upd);
        assertThat(updated).isPresent();
        assertThat(updated.get().name()).isEqualTo("Alice2");

        // delete
        boolean deleted = service.delete(id);
        assertThat(deleted).isTrue();
        assertThat(service.getById(id)).isEmpty();
    }
}
