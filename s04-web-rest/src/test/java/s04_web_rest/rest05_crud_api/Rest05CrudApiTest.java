package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest05CrudApi — warstwa kontrolera. Wycinek: {@code @WebMvcTest} + {@code @MockitoBean} serwisu.
 * Serwis jest PODMIENIONY atrapą (mock), więc test sprawdza tylko pracę kontrolera: który kod HTTP, jaki JSON,
 * czy wywołał serwis (i z jakimi argumentami) — bez prawdziwych danych.
 */
@WebMvcTest(ProductController.class)
class Rest05CrudApiTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean   // bean-atrapa zamiast prawdziwego ProductService
    ProductService service;

    private static Product kawa() {
        return new Product(1, "Kawa", new BigDecimal("24.90"), "SKU-1");
    }

    @Test
    @DisplayName("GET lista: 200 i JSON bez pola internalCode (DTO wyjściowe ukrywa kod wewnętrzny)")
    void list() throws Exception {
        given(service.findAll()).willReturn(List.of(kawa(), new Product(2, "Herbata", new BigDecimal("15.99"), "SKU-2")));

        mockMvc.perform(get("/rest05/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Kawa"))
                .andExpect(jsonPath("$[0].internalCode").doesNotExist());
    }

    @Test
    @DisplayName("GET {id}: 200 gdy jest, 404 bez ciała gdy serwis zwraca puste Optional")
    void getOneAndNotFound() throws Exception {
        given(service.find(1)).willReturn(Optional.of(kawa()));
        given(service.find(99)).willReturn(Optional.empty());

        mockMvc.perform(get("/rest05/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(24.90));
        mockMvc.perform(get("/rest05/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("POST: 201, Location z id z serwisu; serwis dostał dokładnie to, co wysłał klient")
    void create() throws Exception {
        given(service.create(any())).willReturn(new Product(3, "Ciastka", new BigDecimal("12.50"), "SKU-3"));

        mockMvc.perform(post("/rest05/products").contentType("application/json")
                        .content("{\"name\":\"Ciastka\",\"price\":12.50}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/rest05/products/3"))
                .andExpect(jsonPath("$.id").value(3));

        verify(service).create(new ProductRequest("Ciastka", new BigDecimal("12.50")));
    }

    @Test
    @DisplayName("POST bez nazwy lub z ujemną ceną: 400 i serwis w ogóle nie jest wołany")
    void createInvalid() throws Exception {
        mockMvc.perform(post("/rest05/products").contentType("application/json").content("{\"price\":12.50}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/rest05/products").contentType("application/json")
                        .content("{\"name\":\"Ciastka\",\"price\":-1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT {id}: 200 z nowymi danymi albo 404")
    void replace() throws Exception {
        given(service.replace(1, new ProductRequest("Kawa ziarnista", new BigDecimal("39.00"))))
                .willReturn(Optional.of(new Product(1, "Kawa ziarnista", new BigDecimal("39.00"), "SKU-1")));
        given(service.replace(eq(99L), any())).willReturn(Optional.empty());

        mockMvc.perform(put("/rest05/products/1").contentType("application/json")
                        .content("{\"name\":\"Kawa ziarnista\",\"price\":39.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa ziarnista"));
        mockMvc.perform(put("/rest05/products/99").contentType("application/json")
                        .content("{\"name\":\"X\",\"price\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH {id}: zmienia tylko cenę (serwis dostaje samą cenę); brak ceny → 400")
    void patchPrice() throws Exception {
        given(service.changePrice(1, new BigDecimal("19.90")))
                .willReturn(Optional.of(new Product(1, "Kawa", new BigDecimal("19.90"), "SKU-1")));

        mockMvc.perform(patch("/rest05/products/1").contentType("application/json").content("{\"price\":19.90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(19.90))
                .andExpect(jsonPath("$.name").value("Kawa"));

        mockMvc.perform(patch("/rest05/products/1").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).changePrice(1, null);
    }

    @Test
    @DisplayName("DELETE {id}: 204 gdy usunięto, 404 gdy nie było")
    void deleteOneAndNotFound() throws Exception {
        given(service.delete(1)).willReturn(true);
        given(service.delete(99)).willReturn(false);

        mockMvc.perform(delete("/rest05/products/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/rest05/products/99")).andExpect(status().isNotFound());
    }
}
