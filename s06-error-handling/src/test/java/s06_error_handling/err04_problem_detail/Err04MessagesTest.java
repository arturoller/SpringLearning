package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 5 lekcji Err04ProblemDetail: teksty z pliku {@code err04/messages.properties}. Profil {@code err04} ustawia
 * {@code spring.messages.basename}; slice {@code @WebMvcTest} nie ładuje automatycznej konfiguracji komunikatów, więc
 * dołączamy ją jawnie ({@code @ImportAutoConfiguration}).
 */
@WebMvcTest(ShopController.class)
@ActiveProfiles("err04")
@ImportAutoConfiguration(MessageSourceAutoConfiguration.class)
class Err04MessagesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 5: tytuł, opis i type wyjątku Springa (405) pochodzą z pliku, a argumenty {0} i {1} są podstawione")
    void messagesForSpringsException() throws Exception {
        mockMvc.perform(delete("/err04/products/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Niedozwolona metoda HTTP"))
                .andExpect(jsonPath("$.detail").value("Metoda DELETE nie jest tu obsługiwana. Dozwolone metody: [GET]"))
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/method-not-allowed"));
    }

    @Test
    @DisplayName("Sekcja 5: własny wyjątek z ErrorResponseException też ma teksty z pliku (tytuł, opis z argumentem {0})")
    void messagesForOwnException() throws Exception {
        mockMvc.perform(get("/err04/products/1/archive"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.title").value("Produkt wycofany"))
                .andExpect(jsonPath("$.detail").value("Produkt 1 został wycofany z oferty i nie wróci do sprzedaży"))
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/product-archived"));   // nie ma w pliku = z kodu
    }

    @Test
    @DisplayName("Sekcja 5: handlery napisane przez nas (ProblemDetail w kodzie) plik z komunikatami pomija")
    void ownHandlersIgnoreMessageFile() throws Exception {
        mockMvc.perform(get("/err04/products/9"))
                .andExpect(jsonPath("$.title").value("Produkt nie istnieje"))
                .andExpect(jsonPath("$.detail").value("Nie ma produktu o id 9"));
    }
}
