package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Testy-ĆWICZENIA lekcji Test02MockitoBeans: TY piszesz test (usuń linię z UnsupportedOperationException i napisz
 * go; adnotacje wycinka dodaj w osobnej klasie). @Tag("cwiczenie") — uruchamiasz sam (▶ obok metody).
 * Rozwiązania wzorcowe: pakiet s11_testing.solutions.test02_mockito_beans.
 */
@Tag("cwiczenie")
class Test02MockitoExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: @WebMvcTest + @MockitoBean — GET /test02/products/1 → 200 i nazwa z atrapy serwisu; verify wywołania")
    void exercise1() {
        // TODO: given(service.find(1L)).willReturn(new Product(...)); mockMvc.perform(get(...)); verify(service).find(1L)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 2: zwykły test z fake — PriceClient jako lambda zwraca 30,00 i serwis zapisuje nową cenę")
    void exercise2() {
        // TODO: new ProductService(name -> new BigDecimal("30.00"), new AuditLog(), Clock.fixed(...)); refreshPrice(2)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3: @SpringBootTest + @MockitoBean PriceClient z willThrow — cena bez zmian, w dzienniku porażka")
    void exercise3() {
        // TODO: @MockitoBean PriceClient; given(...).willThrow(new PriceUnavailableException("timeout"));
        //       assertThatThrownBy(...); sprawdź cenę produktu 2 i audit.events() (pamiętaj o service.reset() i audit.clear())
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 4: @MockitoSpyBean AuditLog + ArgumentCaptor — kolejność zdarzeń po create i refreshPrice")
    void exercise4() {
        // TODO: @MockitoSpyBean AuditLog + @MockitoBean PriceClient; verify(audit, times(2)).record(captor.capture());
        //       captor.getAllValues() ma być: ["created:3", "price-refreshed:1"]
        throw new UnsupportedOperationException("TODO");
    }
}
