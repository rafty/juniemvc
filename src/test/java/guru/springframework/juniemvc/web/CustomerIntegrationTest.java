package guru.springframework.juniemvc.web;

import guru.springframework.juniemvc.models.CustomerDtos.CustomerCreateRequest;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerResponse;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("Customer CRUD flow: create -> get -> list -> update -> delete")
    void customerCrudFlow() {
        // create
        CustomerCreateRequest create = new CustomerCreateRequest(
                "Alice",
                "alice@example.com",
                "+1-234",
                "1 Main St",
                null,
                "Springfield",
                "IL",
                "62704"
        );

        ResponseEntity<CustomerResponse> createResp = rest.postForEntity(url("/api/v1/customers"), create, CustomerResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = createResp.getHeaders().getLocation();
        assertThat(location).isNotNull();
        Integer id = createResp.getBody() != null ? createResp.getBody().id() : null;
        assertThat(id).isNotNull();

        // get by id
        ResponseEntity<CustomerResponse> getResp = rest.getForEntity(location, CustomerResponse.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody()).isNotNull();
        assertThat(getResp.getBody().name()).isEqualTo("Alice");

        // list (paged)
        ResponseEntity<String> listResp = rest.getForEntity(url("/api/v1/customers?page=0&size=10"), String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).contains("content");

        // update
        CustomerUpdateRequest update = new CustomerUpdateRequest(
                "Alice2",
                null,
                "+1-999",
                "2 Main St",
                null,
                "New City",
                "IL",
                "62705"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CustomerUpdateRequest> entity = new HttpEntity<>(update, headers);
        ResponseEntity<CustomerResponse> putResp = rest.exchange(url("/api/v1/customers/" + id), HttpMethod.PUT, entity, CustomerResponse.class);
        assertThat(putResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(putResp.getBody()).isNotNull();
        assertThat(putResp.getBody().name()).isEqualTo("Alice2");

        // delete
        rest.delete(url("/api/v1/customers/" + id));
        ResponseEntity<CustomerResponse> afterDelete = rest.getForEntity(url("/api/v1/customers/" + id), CustomerResponse.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
