package guru.springframework.juniemvc.controllers;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.services.BeerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/beer")
public class BeerController {

    private final BeerService beerService;

    public BeerController(BeerService beerService) {
        this.beerService = beerService;
    }

    @PostMapping
    public ResponseEntity<Beer> create(@RequestBody Beer beer) {
        Beer saved = beerService.create(beer);
        return ResponseEntity.created(URI.create("/api/v1/beer/" + saved.getId())).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Beer> getById(@PathVariable Integer id) {
        return beerService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<List<Beer>> listAll() {
        return ResponseEntity.ok(beerService.listAll());
    }
}
