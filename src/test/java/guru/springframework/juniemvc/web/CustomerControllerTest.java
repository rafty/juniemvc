package guru.springframework.juniemvc.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.juniemvc.exceptions.CustomerNotFoundException;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerCreateRequest;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerResponse;
import guru.springframework.juniemvc.models.CustomerDtos.CustomerUpdateRequest;
import guru.springframework.juniemvc.services.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
class CustomerControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CustomerService customerService;

    @Test
    @DisplayName("POST /api/v1/customers -> 201 Created with Location and body")
    void create201() throws Exception {
        CustomerCreateRequest req = new CustomerCreateRequest("Alice", null, null, "A1", null, "City", "ST", "00001");
        CustomerResponse resp = new CustomerResponse(1, 0, "Alice", null, null, "A1", null, "City", "ST", "00001", null, null);
        given(customerService.create(any())).willReturn(resp);

        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/customers/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} -> 200 OK")
    void get200() throws Exception {
        CustomerResponse resp = new CustomerResponse(2, 0, "Bob", null, null, "B1", null, "City", "ST", "00001", null, null);
        given(customerService.getById(2)).willReturn(Optional.of(resp));

        mvc.perform(get("/api/v1/customers/{id}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} -> 404 Not Found via exception handler")
    void get404() throws Exception {
        given(customerService.getById(999)).willReturn(Optional.empty());
        mvc.perform(get("/api/v1/customers/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} -> 200 OK when updated, 404 when missing")
    void put200and404() throws Exception {
        CustomerUpdateRequest req = new CustomerUpdateRequest("New", null, null, "NA1", null, "NC", "ST", "00002");
        CustomerResponse updated = new CustomerResponse(5, 0, "New", null, null, "NA1", null, "NC", "ST", "00002", null, null);
        given(customerService.update(eq(5), any())).willReturn(Optional.of(updated));
        given(customerService.update(eq(404), any())).willReturn(Optional.empty());

        mvc.perform(put("/api/v1/customers/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        mvc.perform(put("/api/v1/customers/{id}", 404)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} -> 204 or 404")
    void delete204or404() throws Exception {
        given(customerService.delete(7)).willReturn(true);
        given(customerService.delete(8)).willReturn(false);

        mvc.perform(delete("/api/v1/customers/{id}", 7))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/v1/customers/{id}", 8))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/customers -> 200 page response")
    void list200() throws Exception {
        Page<CustomerResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(customerService.list(any())).willReturn(page);

        mvc.perform(get("/api/v1/customers").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }
}
