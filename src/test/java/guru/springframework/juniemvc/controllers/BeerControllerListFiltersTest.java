package guru.springframework.juniemvc.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.juniemvc.models.BeerDto;
import guru.springframework.juniemvc.services.BeerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BeerController.class)
@Import(BeerControllerListFiltersTest.MockConfig.class)
class BeerControllerListFiltersTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BeerService beerService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        BeerService beerService() {
            return Mockito.mock(BeerService.class);
        }
    }

    private BeerDto sampleBeer(Integer id, String style) {
        return BeerDto.builder()
                .id(id)
                .beerName("Sample")
                .beerStyle(style)
                .upc("U" + id)
                .quantityOnHand(10)
                .price(new BigDecimal("4.50"))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/beer - list paged, with beerStyle only")
    void listPagedWithBeerStyleOnly() throws Exception {
        List<BeerDto> beers = Arrays.asList(sampleBeer(1, "IPA"));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        org.springframework.data.domain.Page<BeerDto> page = new org.springframework.data.domain.PageImpl<>(beers, pageable, 1);
        Mockito.when(beerService.list(any(org.springframework.data.domain.Pageable.class), eq((String) null), eq("IPA")))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/beer")
                        .param("page", "0")
                        .param("size", "5")
                        .param("beerStyle", "IPA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].beerStyle", is("IPA")));
    }

    @Test
    @DisplayName("GET /api/v1/beer - list paged, with beerName and beerStyle")
    void listPagedWithBothFilters() throws Exception {
        List<BeerDto> beers = Arrays.asList(sampleBeer(2, "LAGER"));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<BeerDto> page = new org.springframework.data.domain.PageImpl<>(beers, pageable, 1);
        Mockito.when(beerService.list(any(org.springframework.data.domain.Pageable.class), eq("Cat"), eq("LAGER")))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/beer")
                        .param("page", "0")
                        .param("size", "10")
                        .param("beerName", "Cat")
                        .param("beerStyle", "LAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].beerStyle", is("LAGER")));
    }
}
