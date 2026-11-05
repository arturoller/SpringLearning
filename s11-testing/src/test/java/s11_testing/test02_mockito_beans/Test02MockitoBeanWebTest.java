package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 1 lekcji Test02MockitoBeans: {@code @MockitoBean} w wycinku {@code @WebMvcTest}. Zamiast prawdziwego
 * ProductService Spring wstawia do kontekstu atrapę Mockito; test ustawia jej odpowiedzi (given/willReturn),
 * a po żądaniu sprawdza wywołania (verify) i przechwytuje argumenty (ArgumentCaptor).
 */
@WebMvcTest(ProductController.class)
class Test02MockitoBeanWebTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;   // atrapa podmienia bean ProductService w kontekście testu

    @Test
    @DisplayName("Sekcja 1: given(...).willReturn(...) ustawia odpowiedź atrapy, a kontroler zamienia ją na JSON")
    void stubbedAnswer() throws Exception {
        given(service.find(1L)).willReturn(new Product(1, "Kawa", new BigDecimal("24.90"), NOW));

        mockMvc.perform(get("/test02/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa"));

        verify(service).find(1L);   // kontroler zawołał serwis dokładnie raz z numerem 1
    }

    @Test
    @DisplayName("Sekcja 1: ArgumentCaptor przechwytuje to, co kontroler przekazał serwisowi po przetworzeniu JSON-a")
    void argumentCaptor() throws Exception {
        given(service.create(any(ProductRequest.class)))
                .willReturn(new Product(3, "Ciastka", new BigDecimal("12.50"), NOW));
        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);   // captor = łapacz argumentu

        mockMvc.perform(post("/test02/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ciastka\",\"price\":12.50}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/test02/products/3"));

        then(service).should().create(captor.capture());   // styl BDD: then(...).should() = verify(...)
        assertThat(captor.getValue()).isEqualTo(new ProductRequest("Ciastka", new BigDecimal("12.50")));
    }

    @Test
    @DisplayName("Sekcja 4: willThrow(PriceUnavailableException) → kontroler odpowiada 503 z ProblemDetail (ścieżka błędu)")
    void errorPath() throws Exception {
        given(service.refreshPrice(1L)).willThrow(new PriceUnavailableException("Usługa cen nie odpowiada"));

        mockMvc.perform(post("/test02/products/1/refresh-price"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Usługa cen niedostępna"));
    }

    @Test
    @DisplayName("Sekcja 1: nieustawiona atrapa zwraca null, a verify(never()) potwierdza, że metody nie wołano")
    void unstubbedAndNever() throws Exception {
        mockMvc.perform(get("/test02/products/1")).andExpect(status().isOk());   // service.find zwraca null → pusty JSON

        verify(service, never()).create(any());
    }
}
