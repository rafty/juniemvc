package guru.springframework.juniemvc.controllers;

import guru.springframework.juniemvc.models.BeerDto;
import guru.springframework.juniemvc.services.BeerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/beer")
class BeerController {

    private final BeerService beerService;

    BeerController(BeerService beerService) {
        this.beerService = beerService;
    }

    @PostMapping
    public ResponseEntity<BeerDto> create(@Valid @RequestBody BeerDto beerDto) {
        BeerDto saved = beerService.create(beerDto);
        return ResponseEntity.created(URI.create("/api/v1/beer/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeerDto> getById(@PathVariable Integer id) {
        return beerService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<BeerDto>> list(
            org.springframework.data.domain.Pageable pageable,
            @RequestParam(value = "beerName", required = false) String beerName) {
        return ResponseEntity.ok(beerService.list(pageable, beerName));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BeerDto> update(@PathVariable Integer id, @Valid @RequestBody BeerDto beerDto) {
        return beerService.update(id, beerDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        boolean deleted = beerService.delete(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
