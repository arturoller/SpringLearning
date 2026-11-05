package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Sekcja 5 lekcji Test02MockitoBeans: nie wszystko trzeba mockować. Interfejs z jedną metodą można zaimplementować
 * lambdą (fake = prosta, działająca podróbka), co daje test sprawdzający ZACHOWANIE (wynik), a nie to, które metody
 * wywołano. Zwykły JUnit, bez Springa i bez kontekstu.
 */
class Test02FakeVsMockTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("Sekcja 5: fake (lambda) zamiast atrapy — test sprawdza wynik: nowa cena i wpis w dzienniku")
    void fakeClient() {
        AuditLog audit = new AuditLog();   // prawdziwy, tani obiekt — po co go mockować?
        ProductService service = new ProductService(name -> new BigDecimal("26.50"), audit, clock);

        Product updated = service.refreshPrice(1L);

        assertThat(updated.price()).isEqualByComparingTo("26.50");
        assertThat(service.find(1L).price()).isEqualByComparingTo("26.50");
        assertThat(audit.events()).containsExactly("price-refreshed:1");
    }

    @Test
    @DisplayName("Sekcja 5: fake rzucający wyjątek — ścieżka błędu bez Mockito; cena zostaje stara")
    void failingFake() {
        AuditLog audit = new AuditLog();
        PriceClient down = name -> {
            throw new PriceUnavailableException("brak połączenia");
        };
        ProductService service = new ProductService(down, audit, clock);

        assertThatThrownBy(() -> service.refreshPrice(1L)).isInstanceOf(PriceUnavailableException.class);

        assertThat(service.find(1L).price()).isEqualByComparingTo("24.90");
        assertThat(audit.events()).containsExactly("price-refresh-failed:1");
    }

    @Test
    @DisplayName("Sekcja 5: ten sam scenariusz z atrapą Mockito — działa, ale wymaga zapisania, jak atrapa ma się zachować")
    void mockClient() {
        PriceClient client = Mockito.mock(PriceClient.class);
        given(client.currentPrice("Kawa")).willReturn(new BigDecimal("26.50"));
        ProductService service = new ProductService(client, new AuditLog(), clock);

        assertThat(service.refreshPrice(1L).price()).isEqualByComparingTo("26.50");
    }

    @Test
    @DisplayName("Sekcja 5: PUŁAPKA — mock rekordu istnieje (Mockito 5), ale jego akcesory zwracają null i 0, bez sensu")
    void mockingValueTypeIsPointless() {
        Product mock = Mockito.mock(Product.class);   // rekord jest final — Mockito 5 potrafi i takie

        assertThat(mock.name()).isNull();
        assertThat(mock.id()).isZero();
        // zamiast tego: new Product(1, "Kawa", new BigDecimal("24.90"), Instant.now())
    }
}
