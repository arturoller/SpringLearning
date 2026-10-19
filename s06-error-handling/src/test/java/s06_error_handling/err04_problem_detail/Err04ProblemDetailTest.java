package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Err04ProblemDetail z WŁASNĄ poradą (ProblemAdvice rozszerza ResponseEntityExceptionHandler).
 * Porada sama buduje odpowiedzi, więc MockMvc je widzi (nie potrzeba /error ani prawdziwego serwera).
 */
@SpringBootTest
@AutoConfigureMockMvc
class Err04ProblemDetailTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. ProblemDetail z handlera: status, typ treści application/problem+json i pola RFC 9457")
    void problemDetailFromHandler() throws Exception {
        mockMvc.perform(get("/api/products/XXX"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://sklep.example.test/problemy/produkt-nie-istnieje"))
                .andExpect(jsonPath("$.title").value("Produkt nie istnieje"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Nie ma produktu o SKU XXX"))
                .andExpect(jsonPath("$.instance").value("/api/products/XXX"));   // Spring sam wpisał adres zapytania
    }

    @Test
    @DisplayName("2. Własne pola (sku) trafiają na wierzch JSON-a, nie do zagnieżdżonego obiektu properties")
    void extensionMembersAreFlattened() throws Exception {
        mockMvc.perform(get("/api/products/XXX"))
                .andExpect(jsonPath("$.sku").value("XXX"))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    @DisplayName("3. ErrorResponseException niesie własny ProblemDetail: 409 z polami sku i available")
    void errorResponseException() throws Exception {
        mockMvc.perform(post("/api/products/ELE-001/reserve").param("quantity", "10"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://sklep.example.test/problemy/brak-towaru"))
                .andExpect(jsonPath("$.detail").value("Brak towaru ELE-001: dostępne 7, żądane 10"))
                .andExpect(jsonPath("$.sku").value("ELE-001"))
                .andExpect(jsonPath("$.available").value(7));
    }

    @Test
    @DisplayName("4. Błąd walidacji: standardowy ProblemDetail 400 + nasze pole errors (nadpisany handleMethodArgumentNotValid)")
    void validationWithErrorsMember() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \",\"price\":-2}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Niepoprawne dane"))
                .andExpect(jsonPath("$.errors.name").value("Nazwa nie może być pusta"))
                .andExpect(jsonPath("$.errors.price").value("Cena musi być dodatnia"));
    }

    @Test
    @DisplayName("5. Klasa bazowa sama obsługuje wyjątki Springa: 405 (z nagłówkiem Allow), 400, 415, 404 — wszystko jako problem+json")
    void inheritedHandlersForSpringExceptions() throws Exception {
        mockMvc.perform(delete("/api/products/ELE-001"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405));
        mockMvc.perform(post("/api/products/ELE-001/reserve"))   // brak wymaganego parametru quantity
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(post("/api/products").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/nie-ma-takiego"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("6. Domyślny type to about:blank, więc w JSON-ie pola type po prostu nie ma")
    void typeAboutBlankIsOmitted() throws Exception {
        mockMvc.perform(delete("/api/products/ELE-001"))
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.instance").value("/api/products/ELE-001"));
    }

    @Test
    @DisplayName("7. Siatka bezpieczeństwa: nieznany wyjątek → 500 z ogólnym opisem, bez tekstu z wnętrza aplikacji")
    void unexpectedExceptionGives500Problem() throws Exception {
        mockMvc.perform(get("/api/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Błąd wewnętrzny"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("jdbc:h2"))));
    }

    @Test
    @DisplayName("8. MessageSource: title i detail z plików err04_messages*.properties zależnie od Accept-Language")
    void messagesFromMessageSource() throws Exception {
        mockMvc.perform(delete("/api/products/ELE-001").header("Accept-Language", "en"))
                .andExpect(jsonPath("$.title").value("Method not allowed"))
                .andExpect(jsonPath("$.detail").value("Method DELETE is not allowed here. Allowed: [GET]."));
        mockMvc.perform(delete("/api/products/ELE-001").header("Accept-Language", "pl"))
                .andExpect(jsonPath("$.title").value("Metoda niedozwolona"))
                .andExpect(jsonPath("$.detail").value("Metoda DELETE nie jest obsługiwana dla tego adresu. Dozwolone: [GET]."));
        // język bez własnego pliku (de) → plik domyślny (polski), a NIE język systemu — dzięki setFallbackToSystemLocale(false)
        mockMvc.perform(delete("/api/products/ELE-001").header("Accept-Language", "de"))
                .andExpect(jsonPath("$.title").value("Metoda niedozwolona"));
    }

    @Test
    @DisplayName("9. MessageSource nadpisuje title także dla naszego ErrorResponseException (klucz problemDetail.title.<klasa>)")
    void messageSourceOverridesTitleOfErrorResponseException() throws Exception {
        mockMvc.perform(post("/api/products/ELE-001/reserve").param("quantity", "10").header("Accept-Language", "en"))
                .andExpect(jsonPath("$.title").value("Out of stock (from messages file)"));
        mockMvc.perform(post("/api/products/ELE-001/reserve").param("quantity", "10").header("Accept-Language", "pl"))
                .andExpect(jsonPath("$.title").value("Brak towaru w magazynie (z pliku komunikatów)"));
    }

    @Test
    @DisplayName("10. ResponseStatusException też staje się problem+json (jest podklasą ErrorResponseException)")
    void responseStatusExceptionBecomesProblem() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001/archive"))
                .andExpect(status().isGone())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Produkt ELE-001 został zarchiwizowany"));
    }
}
