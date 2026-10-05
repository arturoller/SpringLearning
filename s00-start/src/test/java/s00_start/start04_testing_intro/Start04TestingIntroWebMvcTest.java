package s00_start.start04_testing_intro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 4 lekcji Start04TestingIntro: {@code @WebMvcTest} — WYCINEK kontekstu z samym PriceController.
 * Serwisy nie powstają, więc PriceCalculator podajemy jako atrapę: {@code @MockitoBean} (Boot 4; dawne @MockBean usunięto).
 */
@WebMvcTest(PriceController.class)
class Start04TestingIntroWebMvcTest {

    @Autowired
    MockMvc mockMvc;   // w @WebMvcTest MockMvc jest dostępne od razu, bez @AutoConfigureMockMvc

    @MockitoBean
    PriceCalculator calculator;   // atrapa: każda metoda zwraca 0, dopóki nie powiemy inaczej (given...)

    @Test
    @DisplayName("4. Kontroler oddaje w JSON to, co zwrócił serwis — tu „liczbę z kosmosu” z atrapy (pytanie 6)")
    void controllerUsesService() throws Exception {
        given(calculator.gross(10_000, 23)).willReturn(99_999);   // stub: ustalona odpowiedź atrapy

        mockMvc.perform(get("/api/price").param("net", "10000").param("vat", "23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gross").value(99999));

        verify(calculator).gross(10_000, 23);   // kontroler zapytał serwis dokładnie o to
    }

    @Test
    @DisplayName("4. Wyjątek z serwisu kontroler zamienia na 400 — sprawdzamy obsługę błędów bez prawdziwych obliczeń")
    void controllerMapsExceptionTo400() throws Exception {
        given(calculator.gross(10_000, 7)).willThrow(new IllegalArgumentException("Nieznana stawka VAT: 7"));

        mockMvc.perform(get("/api/price").param("net", "10000").param("vat", "7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nieznana stawka VAT: 7"));
    }
}
