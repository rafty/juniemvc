package guru.springframework.juniemvc.services.impl;

import guru.springframework.juniemvc.entities.Customer;
import guru.springframework.juniemvc.mappers.CustomerMapper;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerCreateRequest;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerResponse;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerUpdateRequest;
import guru.springframework.juniemvc.repositories.CustomerRepository;
import guru.springframework.juniemvc.services.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public CustomerResponse create(CustomerCreateRequest cmd) {
        Customer entity = customerMapper.toEntity(cmd);
        Customer saved = customerRepository.save(entity);
        return customerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerResponse> getById(Integer id) {
        return customerRepository.findById(id).map(customerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(Pageable pageable) {
        Page<Customer> page = customerRepository.findAll(pageable);
        List<CustomerResponse> mapped = page.getContent().stream().map(customerMapper::toResponse).toList();
        return new PageImpl<>(mapped, pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public Optional<CustomerResponse> update(Integer id, CustomerUpdateRequest cmd) {
        return customerRepository.findById(id).map(existing -> {
            customerMapper.updateEntity(existing, cmd);
            Customer saved = customerRepository.save(existing);
            return customerMapper.toResponse(saved);
        });
    }

    @Override
    @Transactional
    public boolean delete(Integer id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
