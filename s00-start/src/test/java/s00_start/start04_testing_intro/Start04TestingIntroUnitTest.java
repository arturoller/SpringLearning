package s00_start.start04_testing_intro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1–2 lekcji Start04TestingIntro: test jednostkowy, BEZ Springa. Brak adnotacji na klasie = nic nie startuje,
 * JUnit po prostu tworzy obiekt tej klasy i woła metody z {@code @Test}.
 */
class Start04TestingIntroUnitTest {

    private final PriceCalculator calculator = new PriceCalculator();   // zwykłe new — żadnego kontenera

    @Test
    @DisplayName("1. given/when/then: 100,00 zł netto + 23% VAT = 123,00 zł brutto")
    void grossWithVat() {
        // given (mając): cenę netto w groszach i stawkę
        int net = 10_000;
        int vat = 23;

        // when (gdy): liczymy brutto
        int gross = calculator.gross(net, vat);

        // then (wtedy): sprawdzamy wynik
        assertThat(gross).isEqualTo(12_300);
    }

    @Test
    @DisplayName("1. Zaokrąglenie do najbliższego grosza: 999 gr + 23% → 1229 gr (pytanie kontrolne 2)")
    void rounding() {
        assertThat(calculator.gross(999, 23)).isEqualTo(1229);
    }

    @Test
    @DisplayName("2. containsExactly: brutto dla wszystkich stawek, w podanej kolejności")
    void allRates() {
        List<Integer> grossForEachRate = List.of(0, 5, 8, 23).stream()
                .map(vat -> calculator.gross(10_000, vat))
                .toList();
        assertThat(grossForEachRate).containsExactly(10_000, 10_500, 10_800, 12_300);
    }

    @Test
    @DisplayName("2. assertThatThrownBy: nieznana stawka VAT kończy się wyjątkiem z czytelnym komunikatem")
    void unknownVat() {
        assertThatThrownBy(() -> calculator.gross(10_000, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VAT");
    }

    @Test
    @DisplayName("2. assertThatThrownBy: ujemna cena też jest odrzucana")
    void negativePrice() {
        assertThatThrownBy(() -> calculator.gross(-1, 23))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cena netto nie może być ujemna: -1");
    }
}
