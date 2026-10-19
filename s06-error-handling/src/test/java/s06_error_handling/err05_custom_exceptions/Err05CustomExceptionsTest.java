package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Err05CustomExceptions. {@code @SpringBootTest} + MockMvc: sprawdzamy całość (kontroler → serwis → wyjątek →
 * porada → odpowiedź i log). Generator identyfikatorów błędów jest podmieniony przez {@code @MockitoBean}
 * (podmiana beana atrapą Mockito), żeby identyfikator był stały i test powtarzalny.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class Err05CustomExceptionsTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ErrorIdGenerator errorIds;

    @BeforeEach
    void fixedErrorId() {
        given(errorIds.next()).willReturn("TEST-0001");
    }

    @Test
    @DisplayName("1. ProductNotFoundException → 404 z kodem PRODUKT_NIE_ISTNIEJE i polem sku")
    void notFound() throws Exception {
        mockMvc.perform(get("/api/products/XXX"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PRODUKT_NIE_ISTNIEJE"))
                .andExpect(jsonPath("$.sku").value("XXX"))
                .andExpect(jsonPath("$.type").value("https://sklep.example.test/problemy/produkt-nie-istnieje"));
        mockMvc.perform(get("/api/products/ELE-001")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. OutOfStockException → 409 Conflict z polami sku i available")
    void outOfStock() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(JSON)
                        .content("{\"customer\":\"Jan Kowalski\",\"lines\":[{\"sku\":\"ELE-001\",\"quantity\":50}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BRAK_TOWARU"))
                .andExpect(jsonPath("$.sku").value("ELE-001"))
                .andExpect(jsonPath("$.available").value(7));
    }

    @Test
    @DisplayName("3. Błąd BIZNESOWY w poprawnym JSON-ie → 422: pusta lista pozycji i nieznany produkt w pozycji (nie 404!)")
    void businessRuleViolation422() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(JSON).content("{\"customer\":\"Jan\",\"lines\":[]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ZAMOWIENIE_NIEPOPRAWNE"))
                .andExpect(jsonPath("$.detail").value("Zamówienie nie ma żadnej pozycji"));
        mockMvc.perform(post("/api/orders").contentType(JSON)
                        .content("{\"customer\":\"Jan\",\"lines\":[{\"sku\":\"ZZZ-1\",\"quantity\":1}]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Nieznany produkt w pozycji zamówienia: ZZZ-1"));
    }

    @Test
    @DisplayName("4. Błąd FORMATU danych → 400 z mapą errors (a brak pola lines to też 400, nie 422)")
    void formatErrors400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(JSON)
                        .content("{\"customer\":\"\",\"lines\":[{\"sku\":\"ELE-001\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customer").value("Podaj imię i nazwisko klienta"))
                .andExpect(jsonPath("$.errors['lines[0].quantity']").value("Ilość musi być dodatnia"));
        mockMvc.perform(post("/api/orders").contentType(JSON).content("{\"customer\":\"Jan\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lines").value("Brak listy pozycji"));
    }

    @Test
    @DisplayName("5. Poprawne zamówienie → 200 (kontrola: serwis nie rzuca wyjątku dla poprawnych danych)")
    void validOrder() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(JSON)
                        .content("{\"customer\":\"Jan\",\"lines\":[{\"sku\":\"ELE-001\",\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ZAM-001"));
    }

    @Test
    @DisplayName("6. Błąd programisty (NullPointerException) → 500 z errorId; w odpowiedzi nic z wnętrza, w logu błąd i stos")
    void unexpectedErrorHasErrorIdAndNoLeak(CapturedOutput output) throws Exception {
        String body = mockMvc.perform(get("/api/bug"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorId").value("TEST-0001"))
                .andExpect(jsonPath("$.title").value("Błąd wewnętrzny"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("NullPointer").doesNotContain("toString").doesNotContain("nothing");
        assertThat(output.getAll()).contains("[BLAD-WEWNETRZNY] errorId=TEST-0001")
                .contains("java.lang.NullPointerException")
                .contains("OrderService.bug");   // fragment stosu wskazujący miejsce błędu
    }

    @Test
    @DisplayName("7. Awaria bazy → 503; klient nie widzi adresu bazy, a w logu jest cały łańcuch przyczyn (Caused by)")
    void infrastructureFailure(CapturedOutput output) throws Exception {
        String body = mockMvc.perform(get("/api/reports/sales"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorId").value("TEST-0001"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("jdbc:").doesNotContain("db-prod");
        assertThat(output.getAll()).contains("Caused by: java.lang.IllegalStateException: Connection refused: jdbc:h2:tcp://db-prod");
    }

    @Test
    @DisplayName("8. Błąd klienta (4xx) trafia do logu jednym krótkim wpisem INFO, bez stosu wyjątku")
    void clientErrorIsLoggedWithoutStack(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/products/XXX")).andExpect(status().isNotFound());

        assertThat(output.getAll()).contains("Błąd klienta [PRODUKT_NIE_ISTNIEJE]: Nie ma produktu o SKU XXX")
                .doesNotContain("ProductNotFoundException");   // sama nazwa klasy pojawiłaby się dopiero w nagłówku stosu
    }
}
