package s00_start.start04_testing_intro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 3 lekcji Start04TestingIntro: {@code @SpringBootTest} uruchamia CAŁY kontekst Start04Application,
 * a {@code @AutoConfigureMockMvc} dodaje MockMvc — zapytania HTTP prosto do aplikacji, bez portu i sieci.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Start04TestingIntroSpringBootTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PriceCalculator calculator;   // PRAWDZIWY bean z kontekstu, nie atrapa

    @Test
    @DisplayName("3. W pełnym kontekście kalkulator to prawdziwy bean — liczy naprawdę")
    void realBean() {
        assertThat(calculator.gross(10_000, 23)).isEqualTo(12_300);
    }

    @Test
    @DisplayName("3. GET /api/price?net=10000&vat=23 → 200 OK i JSON z ceną brutto 12300")
    void priceEndpoint() throws Exception {
        mockMvc.perform(get("/api/price").param("net", "10000").param("vat", "23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.net").value(10000))
                .andExpect(jsonPath("$.vat").value(23))
                .andExpect(jsonPath("$.gross").value(12300));
    }

    @Test
    @DisplayName("3. Nieznana stawka VAT → 400 Bad Request z komunikatem błędu")
    void badVat() throws Exception {
        mockMvc.perform(get("/api/price").param("net", "10000").param("vat", "7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nieznana stawka VAT: 7"));
    }

    @Test
    @DisplayName("3. Brak wymaganego parametru vat → 400 Bad Request (Spring odrzuca zapytanie sam)")
    void missingParameter() throws Exception {
        mockMvc.perform(get("/api/price").param("net", "10000"))
                .andExpect(status().isBadRequest());
    }
}
