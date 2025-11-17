package guru.springframework.juniemvc.web;

import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentRequest;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentResponse;
import guru.springframework.juniemvc.services.BeerOrderShipmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/beer-orders/{beerOrderId}/shipments")
@Validated
class BeerOrderShipmentController {

    private final BeerOrderShipmentService service;

    BeerOrderShipmentController(BeerOrderShipmentService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<BeerOrderShipmentResponse> create(@PathVariable Integer beerOrderId,
                                                     @Valid @RequestBody BeerOrderShipmentRequest request) {
        BeerOrderShipmentResponse response = service.create(beerOrderId, request);
        return ResponseEntity.created(URI.create("/api/v1/beer-orders/" + beerOrderId + "/shipments/" + response.id()))
                .body(response);
    }

    @GetMapping
    ResponseEntity<Page<BeerOrderShipmentResponse>> list(@PathVariable Integer beerOrderId, Pageable pageable) {
        return ResponseEntity.ok(service.list(beerOrderId, pageable));
    }

    @GetMapping("/{id}")
    ResponseEntity<BeerOrderShipmentResponse> getById(@PathVariable Integer beerOrderId, @PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(beerOrderId, id));
    }

    @PutMapping("/{id}")
    ResponseEntity<BeerOrderShipmentResponse> update(@PathVariable Integer beerOrderId,
                                                     @PathVariable Integer id,
                                                     @Valid @RequestBody BeerOrderShipmentRequest request) {
        return ResponseEntity.ok(service.update(beerOrderId, id, request));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Integer beerOrderId, @PathVariable Integer id) {
        service.delete(beerOrderId, id);
        return ResponseEntity.noContent().build();
    }
}
