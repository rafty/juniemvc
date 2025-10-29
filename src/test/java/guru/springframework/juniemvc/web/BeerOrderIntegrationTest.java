package guru.springframework.juniemvc.web;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.models.BeerOrderDtos;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderCreateRequest;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderLineCreateItem;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderResponse;
import guru.springframework.juniemvc.repositories.BeerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BeerOrderIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    BeerRepository beerRepository;

    Integer beerId1;
    Integer beerId2;

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        Beer b1 = beerRepository.save(Beer.builder().beerName("IPA").beerStyle("IPA").upc("INT-UP1").price(new BigDecimal("4.50")).build());
        Beer b2 = beerRepository.save(Beer.builder().beerName("Lager").beerStyle("LAGER").upc("INT-UP2").price(new BigDecimal("5.00")).build());
        beerId1 = b1.getId();
        beerId2 = b2.getId();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("POST create: 201 + Location + body")
    void postCreate() {
        BeerOrderCreateRequest req = new BeerOrderCreateRequest(
                "CUST-I",
                new BigDecimal("10.00"),
                List.of(new BeerOrderLineCreateItem(beerId1, 1), new BeerOrderLineCreateItem(beerId2, 2))
        );

        ResponseEntity<BeerOrderResponse> resp = rest.postForEntity(url("/api/v1/beer-orders"), req, BeerOrderResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().getLocation()).isNotNull();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().id()).isNotNull();

        URI location = resp.getHeaders().getLocation();
        ResponseEntity<BeerOrderResponse> getResp = rest.getForEntity(location, BeerOrderResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody()).isNotNull();
        assertThat(getResp.getBody().lines()).isNotNull();
        assertThat(getResp.getBody().lines().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST error: unknown beerId -> 404")
    void postUnknownBeer() {
        BeerOrderCreateRequest req = new BeerOrderCreateRequest(
                null,
                null,
                List.of(new BeerOrderLineCreateItem(999999, 1))
        );

        ResponseEntity<String> resp = rest.postForEntity(url("/api/v1/beer-orders"), req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
