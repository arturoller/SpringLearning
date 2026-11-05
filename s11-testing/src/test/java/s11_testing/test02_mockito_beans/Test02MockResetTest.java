package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 3 lekcji Test02MockitoBeans: reset atrap między testami i atrapa używana z klas {@code @Nested}.
 * Kontekst jest wspólny dla wielu testów (pamięć podręczna), więc atrapa też — Spring musi ją sam "czyścić".
 * UWAGA: kolejność testów jest tu USTALONA ({@code @Order}) celowo, bo pokazujemy właśnie skutek wspólnego stanu.
 */
@WebMvcTest(ProductController.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Test02MockResetTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean   // domyślnie reset = MockReset.AFTER: po każdym teście atrapa wraca do stanu "świeżej"
    ProductService service;

    @MockitoBean(reset = MockReset.NONE)   // NONE: nikt jej nie czyści — wszystko przecieka do następnych testów
    AuditLog neverReset;

    @Test
    @DisplayName("Sekcja 3: domyślną wartością reset w @MockitoBean jest MockReset.AFTER")
    void defaultResetIsAfter() throws Exception {
        assertThat(MockitoBean.class.getMethod("reset").getDefaultValue()).isEqualTo(MockReset.AFTER);
    }

    @Test
    @Order(1)
    @DisplayName("Sekcja 3 (test 1 z 2): ustawiamy odpowiedź atrapy i wołamy obie atrapy")
    void firstTestStubsAndCalls() throws Exception {
        given(service.find(1L)).willReturn(new Product(1, "Kawa", new BigDecimal("24.90"), Instant.EPOCH));
        neverReset.record("zdarzenie z testu 1");

        mockMvc.perform(get("/test02/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa"));
    }

    @Test
    @Order(2)
    @DisplayName("Sekcja 3 (test 2 z 2): atrapa z reset=AFTER jest czysta (nic nie wołano, brak ustawień), a z reset=NONE pamięta test 1")
    void secondTestSeesCleanAndDirtyMock() {
        assertThat(Mockito.mockingDetails(service).getInvocations()).as("wołania z testu 1 skasowane").isEmpty();
        assertThat(service.find(1L)).as("ustawiona odpowiedź skasowana → null").isNull();

        assertThat(Mockito.mockingDetails(neverReset).getInvocations()).as("reset=NONE: wołanie z testu 1 nadal widać")
                .hasSize(1);
    }

    /** Atrapa zadeklarowana w klasie zewnętrznej jest dostępna w klasie {@code @Nested} (konfiguracja się dziedziczy). */
    @Nested
    class InsideNestedClass {

        @Test
        @DisplayName("Sekcja 3: klasa @Nested korzysta z atrapy i MockMvc z klasy zewnętrznej (ten sam kontekst)")
        void nestedUsesOuterMock() throws Exception {
            given(service.find(2L)).willReturn(new Product(2, "Herbata", new BigDecimal("15.99"), Instant.EPOCH));

            mockMvc.perform(get("/test02/products/2")).andExpect(jsonPath("$.name").value("Herbata"));
        }
    }
}
