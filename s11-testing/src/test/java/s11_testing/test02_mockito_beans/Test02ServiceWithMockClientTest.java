package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Sekcje 1 i 4 lekcji Test02MockitoBeans: PRAWDZIWY serwis z kontekstu Springa, ale zewnętrzna usługa cen to atrapa
 * ({@code @MockitoBean PriceClient}) — żadnej sieci. {@code @SpringBootTest} (pełny kontekst) jest tu potrzebny, bo
 * sprawdzamy współpracę kilku beanów: serwisu, dziennika i atrapy klienta.
 */
@SpringBootTest
class Test02ServiceWithMockClientTest {

    @MockitoBean
    PriceClient priceClient;   // zastępuje bean RemotePriceClient — jedyny bean typu PriceClient

    @Autowired
    ProductService service;

    @Autowired
    AuditLog audit;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void cleanState() {
        service.reset();   // serwis i dziennik to singletony — stan sprzątamy sami
        audit.clear();
    }

    @Test
    @DisplayName("Sekcja 1: @MockitoBean PriceClient ZASTĘPUJE bean RemotePriceClient (nie dokłada drugiego)")
    void mockReplacesTheRealBean() {
        assertThat(context.getBeanNamesForType(PriceClient.class)).hasSize(1);
        assertThat(context.getBean(PriceClient.class)).isSameAs(priceClient);
        assertThat(Mockito.mockingDetails(priceClient).isMock()).isTrue();
        assertThat(context.getBeanNamesForType(RemotePriceClient.class)).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 4: usługa zwraca nową cenę → serwis ją zapisuje i dopisuje zdarzenie do dziennika")
    void happyPath() {
        given(priceClient.currentPrice("Kawa")).willReturn(new BigDecimal("26.50"));

        Product updated = service.refreshPrice(1L);

        assertThat(updated.price()).isEqualByComparingTo("26.50");
        assertThat(audit.events()).containsExactly("price-refreshed:1");
        verify(priceClient).currentPrice("Kawa");
    }

    @Test
    @DisplayName("Sekcja 4: willThrow → ścieżka błędu: wyjątek leci dalej, cena zostaje stara, w dzienniku jest porażka")
    void errorPath() {
        given(priceClient.currentPrice("Kawa")).willThrow(new PriceUnavailableException("timeout"));

        assertThatThrownBy(() -> service.refreshPrice(1L))
                .isInstanceOf(PriceUnavailableException.class)
                .hasMessage("timeout");

        assertThat(service.find(1L).price()).isEqualByComparingTo("24.90");
        assertThat(audit.events()).containsExactly("price-refresh-failed:1");
    }

    @Test
    @DisplayName("Sekcja 5: PUŁAPKA — atrapa BEZ ustawionej odpowiedzi zwraca null i serwis zapisuje cenę null")
    void unstubbedMockReturnsNull() {
        Product updated = service.refreshPrice(2L);   // nikt nie powiedział atrapie, co zwrócić

        assertThat(updated.price()).isNull();
    }
}
