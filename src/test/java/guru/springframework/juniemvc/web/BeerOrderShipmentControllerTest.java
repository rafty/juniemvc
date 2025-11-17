package guru.springframework.juniemvc.web;

import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentRequest;
import guru.springframework.juniemvc.models.BeerOrderShipmentDtos.BeerOrderShipmentResponse;
import guru.springframework.juniemvc.services.BeerOrderShipmentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BeerOrderShipmentController.class)
class BeerOrderShipmentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    BeerOrderShipmentService service;

    @Test
    void create_returns201AndLocation() throws Exception {
        Integer orderId = 1;
        Mockito.when(service.create(eq(orderId), any(BeerOrderShipmentRequest.class)))
                .thenReturn(new BeerOrderShipmentResponse(10, LocalDate.of(2025,1,2), "UPS", "TN"));

        String json = "{\n  \"shipmentDate\": \"2025-01-02\",\n  \"carrier\": \"UPS\",\n  \"trackingNumber\": \"TN\"\n}";

        mockMvc.perform(post("/api/v1/beer-orders/{beerOrderId}/shipments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/beer-orders/1/shipments/10"))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getById_returns200() throws Exception {
        Integer orderId = 1;
        Integer shipmentId = 2;
        Mockito.when(service.getById(orderId, shipmentId))
                .thenReturn(new BeerOrderShipmentResponse(shipmentId, LocalDate.of(2025,1,2), "UPS", "TN"));

        mockMvc.perform(get("/api/v1/beer-orders/{beerOrderId}/shipments/{id}", orderId, shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }
}
