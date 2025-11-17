package guru.springframework.juniemvc.services.impl;

import guru.springframework.juniemvc.entities.Customer;
import guru.springframework.juniemvc.mappers.CustomerMapper;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerCreateRequest;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerResponse;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerUpdateRequest;
import guru.springframework.juniemvc.repositories.CustomerRepository;
import guru.springframework.juniemvc.services.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomerServiceImplTest {

    CustomerRepository repository;
    CustomerMapper mapper;
    CustomerService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        mapper = mock(CustomerMapper.class);
        service = new CustomerServiceImpl(repository, mapper);
    }

    @Test
    void create_mapsAndSaves() {
        CustomerCreateRequest req = new CustomerCreateRequest("A", null, null, "A1", null, "C", "S", "P");
        Customer entity = new Customer();
        Customer saved = new Customer();
        saved.setId(10);
        CustomerResponse resp = new CustomerResponse(10, 0, "A", null, null, "A1", null, "C", "S", "P", null, null);

        when(mapper.toEntity(req)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(resp);

        CustomerResponse out = service.create(req);
        assertThat(out.id()).isEqualTo(10);
        verify(repository).save(entity);
    }

    @Test
    void getById_mapsOptional() {
        Customer cust = new Customer();
        when(repository.findById(1)).thenReturn(Optional.of(cust));
        when(mapper.toResponse(cust)).thenReturn(new CustomerResponse(1, null, null, null, null, null, null, null, null, null, null, null));
        Optional<CustomerResponse> res = service.getById(1);
        assertThat(res).isPresent();
    }

    @Test
    void list_paginatesAndMaps() {
        Customer c1 = new Customer();
        when(repository.findAll(PageRequest.of(0, 2))).thenReturn(new PageImpl<>(List.of(c1), PageRequest.of(0,2), 1));
        when(mapper.toResponse(c1)).thenReturn(new CustomerResponse(1, null, null, null, null, null, null, null, null, null, null, null));
        Page<CustomerResponse> page = service.list(PageRequest.of(0, 2));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void update_whenFound_updatesAndSaves() {
        Customer existing = new Customer();
        existing.setId(5); // mimic JPA-managed entity loaded with ID 5
        when(repository.findById(5)).thenReturn(Optional.of(existing));
        Customer saved = new Customer(); saved.setId(5);
        when(repository.save(existing)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(new CustomerResponse(5, null, null, null, null, null, null, null, null, null, null, null));

        Optional<CustomerResponse> res = service.update(5, new CustomerUpdateRequest("n", null, null, "a1", null, "c", "s", "p"));
        assertThat(res).isPresent();
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5);
        verify(mapper).updateEntity(existing, new CustomerUpdateRequest("n", null, null, "a1", null, "c", "s", "p"));
    }

    @Test
    void update_whenNotFound_returnsEmpty() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        Optional<CustomerResponse> res = service.update(99, new CustomerUpdateRequest("n", null, null, "a1", null, "c", "s", "p"));
        assertThat(res).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void delete_whenExists_deletesAndReturnsTrue() {
        when(repository.existsById(7)).thenReturn(true);
        boolean deleted = service.delete(7);
        assertThat(deleted).isTrue();
        verify(repository).deleteById(7);
    }

    @Test
    void delete_whenNotExists_returnsFalse() {
        when(repository.existsById(8)).thenReturn(false);
        boolean deleted = service.delete(8);
        assertThat(deleted).isFalse();
        verify(repository, never()).deleteById(any());
    }
}
