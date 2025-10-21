package guru.springframework.juniemvc.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.juniemvc.entities.Beer;
import guru.springframework.juniemvc.services.BeerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BeerController.class)
class BeerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    BeerService beerService;

    private Beer sampleBeer(Integer id) {
        return Beer.builder()
                .id(id)
                .beerName("Test Lager")
                .beerStyle("LAGER")
                .upc("12345")
                .quantityOnHand(50)
                .price(new BigDecimal("9.99"))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/beer - create")
    void createBeer() throws Exception {
        Beer toCreate = sampleBeer(10);
        Mockito.when(beerService.create(any(Beer.class))).thenReturn(toCreate);

        mockMvc.perform(post("/api/v1/beer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toCreate)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/beer/10"))
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.beerName", is("Test Lager")));
    }

    @Test
    @DisplayName("GET /api/v1/beer/{id} - found")
    void getByIdFound() throws Exception {
        Mockito.when(beerService.getById(eq(1))).thenReturn(Optional.of(sampleBeer(1)));

        mockMvc.perform(get("/api/v1/beer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.beerStyle", is("LAGER")));
    }

    @Test
    @DisplayName("GET /api/v1/beer/{id} - not found")
    void getByIdNotFound() throws Exception {
        Mockito.when(beerService.getById(eq(999))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/beer/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/beer - list all")
    void listAll() throws Exception {
        List<Beer> beers = Arrays.asList(sampleBeer(1), sampleBeer(2));
        Mockito.when(beerService.listAll()).thenReturn(beers);

        mockMvc.perform(get("/api/v1/beer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }
}
