package guru.springframework.juniemvc.web;

import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderCreateRequest;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderLineCreateItem;
import guru.springframework.juniemvc.models.BeerOrderDtos.BeerOrderResponse;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentRequest;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentResponse;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BeerOrderShipmentIntegrationTest {

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
        Beer b1 = beerRepository.save(Beer.builder().beerName("IPA").beerStyle("IPA").upc("SHIP-UP1").price(new BigDecimal("4.50")).build());
        Beer b2 = beerRepository.save(Beer.builder().beerName("Lager").beerStyle("LAGER").upc("SHIP-UP2").price(new BigDecimal("5.00")).build());
        beerId1 = b1.getId();
        beerId2 = b2.getId();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("BeerOrderShipment CRUD happy path")
    void shipmentCrudHappyPath() {
        // 1) Create an order
        BeerOrderCreateRequest orderReq = new BeerOrderCreateRequest(
                "SHIP-ORDER",
                new BigDecimal("9.50"),
                List.of(new BeerOrderLineCreateItem(beerId1, 1), new BeerOrderLineCreateItem(beerId2, 1))
        );
        ResponseEntity<BeerOrderResponse> orderResp = rest.postForEntity(url("/api/v1/beer-orders"), orderReq, BeerOrderResponse.class);
        assertThat(orderResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(orderResp.getBody()).isNotNull();
        Integer orderId = orderResp.getBody().id();
        assertThat(orderId).isNotNull();

        // 2) Create a shipment for the order
        BeerOrderShipmentRequest shipReq = new BeerOrderShipmentRequest(LocalDate.now(), "UPS", "1Z999");
        ResponseEntity<BeerOrderShipmentResponse> shipResp = rest.postForEntity(url("/api/v1/beer-orders/" + orderId + "/shipments"), shipReq, BeerOrderShipmentResponse.class);
        assertThat(shipResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(shipResp.getHeaders().getLocation()).isNotNull();
        assertThat(shipResp.getBody()).isNotNull();
        Integer shipmentId = shipResp.getBody().id();
        assertThat(shipmentId).isNotNull();

        // 3) List shipments
        ResponseEntity<String> listResp = rest.getForEntity(url("/api/v1/beer-orders/" + orderId + "/shipments"), String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4) Get by id
        ResponseEntity<BeerOrderShipmentResponse> getResp = rest.getForEntity(url("/api/v1/beer-orders/" + orderId + "/shipments/" + shipmentId), BeerOrderShipmentResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody()).isNotNull();
        assertThat(getResp.getBody().trackingNumber()).isEqualTo("1Z999");

        // 5) Update
        BeerOrderShipmentRequest updReq = new BeerOrderShipmentRequest(shipReq.shipmentDate(), shipReq.carrier(), "UPDATED-TN");
        ResponseEntity<BeerOrderShipmentResponse> putResp = rest.exchange(url("/api/v1/beer-orders/" + orderId + "/shipments/" + shipmentId), HttpMethod.PUT, new HttpEntity<>(updReq), BeerOrderShipmentResponse.class);
        assertThat(putResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(putResp.getBody()).isNotNull();
        assertThat(putResp.getBody().trackingNumber()).isEqualTo("UPDATED-TN");

        // 6) Delete
        ResponseEntity<Void> delResp = rest.exchange(url("/api/v1/beer-orders/" + orderId + "/shipments/" + shipmentId), HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(delResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
