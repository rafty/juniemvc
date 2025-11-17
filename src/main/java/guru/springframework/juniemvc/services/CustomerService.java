package guru.springframework.juniemvc.services;

import guru.springframework.juniemvc.models.CustomerDtos.CustomerCreateRequest;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerResponse;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CustomerService {

    @Transactional
    CustomerResponse create(CustomerCreateRequest cmd);

    @Transactional(readOnly = true)
    Optional<CustomerResponse> getById(Integer id);

    @Transactional(readOnly = true)
    Page<CustomerResponse> list(Pageable pageable);

    @Transactional
    Optional<CustomerResponse> update(Integer id, CustomerUpdateRequest cmd);

    @Transactional
    boolean delete(Integer id);
}
