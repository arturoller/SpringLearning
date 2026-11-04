package s11_testing.solutions.test02_mockito_beans;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import s11_testing.test02_mockito_beans.AuditLog;
import s11_testing.test02_mockito_beans.ClockConfig;
import s11_testing.test02_mockito_beans.PriceClient;
import s11_testing.test02_mockito_beans.PriceUnavailableException;
import s11_testing.test02_mockito_beans.Product;
import s11_testing.test02_mockito_beans.ProductController;
import s11_testing.test02_mockito_beans.ProductErrorAdvice;
import s11_testing.test02_mockito_beans.ProductRequest;
import s11_testing.test02_mockito_beans.ProductService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rozwiązania wzorcowe ćwiczeń Test02MockitoBeans. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
class Test02MockitoSolutionsTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    @Nested
    @WebMvcTest
    @Import({ProductController.class, ProductErrorAdvice.class})
    class Exercise1 {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        ProductService service;

        @Test
        @DisplayName("Wzorzec 1: GET → 200 i nazwa z atrapy; serwis zawołany z numerem 1")
        void stubAndVerify() throws Exception {
            given(service.find(1L)).willReturn(new Product(1, "Kawa", new BigDecimal("24.90"), NOW));

            mockMvc.perform(get("/test02/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Kawa"));

            verify(service).find(1L);
        }
    }

    @Nested
    class Exercise2 {

        @Test
        @DisplayName("Wzorzec 2: fake PriceClient (lambda) zwraca 30,00 i serwis zapisuje nową cenę")
        void fake() {
            ProductService service = new ProductService(name -> new BigDecimal("30.00"), new AuditLog(),
                    Clock.fixed(NOW, ZoneOffset.UTC));

            service.refreshPrice(2L);

            assertThat(service.find(2L).price()).isEqualByComparingTo("30.00");
        }
    }

    @Nested
    @SpringBootTest
    @Import({ProductService.class, AuditLog.class, ClockConfig.class})
    class Exercises3And4 {

        @MockitoBean
        PriceClient priceClient;

        @MockitoSpyBean
        AuditLog audit;

        @Autowired
        ProductService service;

        @BeforeEach
        void cleanState() {
            service.reset();
            audit.clear();
        }

        @Test
        @DisplayName("Wzorzec 3: willThrow → wyjątek, cena produktu 2 bez zmian, w dzienniku porażka")
        void errorPath() {
            given(priceClient.currentPrice("Herbata")).willThrow(new PriceUnavailableException("timeout"));

            assertThatThrownBy(() -> service.refreshPrice(2L)).isInstanceOf(PriceUnavailableException.class);

            assertThat(service.find(2L).price()).isEqualByComparingTo("15.99");
            assertThat(audit.events()).containsExactly("price-refresh-failed:2");
        }

        @Test
        @DisplayName("Wzorzec 4: captor na szpiegu — kolejność zdarzeń po create i refreshPrice")
        void captor() {
            given(priceClient.currentPrice("Kawa")).willReturn(new BigDecimal("26.50"));
            ArgumentCaptor<String> events = ArgumentCaptor.forClass(String.class);

            service.create(new ProductRequest("Ciastka", new BigDecimal("12.50")));
            service.refreshPrice(1L);

            verify(audit, times(2)).record(events.capture());
            assertThat(events.getAllValues()).containsExactly("created:3", "price-refreshed:1");
        }
    }
}
