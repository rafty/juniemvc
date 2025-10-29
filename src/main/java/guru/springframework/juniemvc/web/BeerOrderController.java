package guru.springframework.juniemvc.web;

import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderCreateRequest;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderResponse;
import guru.springframework.juniemvc.services.BeerOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/beer-orders")
@Validated
class BeerOrderController {

    private final BeerOrderService beerOrderService;

    BeerOrderController(BeerOrderService beerOrderService) {
        this.beerOrderService = beerOrderService;
    }

    @PostMapping
    ResponseEntity<BeerOrderResponse> create(@Valid @RequestBody BeerOrderCreateRequest request) {
        BeerOrderResponse response = beerOrderService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/beer-orders/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    ResponseEntity<BeerOrderResponse> getById(@PathVariable Integer id) {
        BeerOrderResponse response = beerOrderService.getById(id);
        return ResponseEntity.ok(response);
    }
}
