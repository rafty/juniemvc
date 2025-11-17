package guru.springframework.juniemvc.web;

import guru.springframework.juniemvc.exceptions.CustomerNotFoundException;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerCreateRequest;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerResponse;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerUpdateRequest;
import guru.springframework.juniemvc.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/customers")
@Validated
class CustomerController {

    private final CustomerService customerService;

    CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse created = customerService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    ResponseEntity<CustomerResponse> getById(@PathVariable Integer id) {
        CustomerResponse body = customerService.getById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return ResponseEntity.ok(body);
    }

    @GetMapping
    ResponseEntity<Page<CustomerResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(customerService.list(pageable));
    }

    @PutMapping("/{id}")
    ResponseEntity<CustomerResponse> update(@PathVariable Integer id,
                                            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerResponse body = customerService.update(id, request)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Integer id) {
        boolean deleted = customerService.delete(id);
        if (deleted) return ResponseEntity.noContent().build();
        throw new CustomerNotFoundException(id);
    }
}
