package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sekcja 2 lekcji Test02MockitoBeans: {@code @MockitoSpyBean} — PRAWDZIWY bean z podsłuchem (spy = szpieg). Metody
 * działają naprawdę (dziennik naprawdę zapisuje zdarzenia), a test może sprawdzić, jak je wołano, albo wybraną
 * metodę podmienić. Różnica wobec atrapy: atrapa jest pusta, szpieg ma całe prawdziwe zachowanie.
 */
@SpringBootTest
class Test02SpyBeanTest {

    @MockitoSpyBean
    AuditLog audit;   // szpieg opakowuje istniejący bean AuditLog — gdyby beana nie było, kontekst by się nie podniósł

    @MockitoBean
    PriceClient priceClient;   // zewnętrzna usługa — atrapa (żadnej sieci)

    @Autowired
    ProductService service;

    @BeforeEach
    void cleanState() {
        service.reset();
        audit.clear();   // singleton: stan z poprzedniego testu zostaje, dlatego sprzątamy
    }

    @Test
    @DisplayName("Sekcja 2: szpieg działa naprawdę (zdarzenie jest w dzienniku) i jednocześnie da się sprawdzić wywołanie")
    void realBehaviourPlusVerify() {
        service.create(new ProductRequest("Ciastka", new BigDecimal("12.50")));

        assertThat(audit.events()).containsExactly("created:3");   // prawdziwa metoda zadziałała
        verify(audit).record("created:3");                         // a my widzimy, jak ją wołano
    }

    @Test
    @DisplayName("Sekcja 2: ArgumentCaptor łapie wszystkie argumenty wywołań szpiega w kolejności")
    void captorOnSpy() {
        given(priceClient.currentPrice("Kawa")).willReturn(new BigDecimal("26.50"));
        ArgumentCaptor<String> events = ArgumentCaptor.forClass(String.class);

        service.create(new ProductRequest("Ciastka", new BigDecimal("12.50")));
        service.refreshPrice(1L);

        verify(audit, times(2)).record(events.capture());
        assertThat(events.getAllValues()).containsExactly("created:3", "price-refreshed:1");
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — when(szpieg.metoda()) WYWOŁUJE prawdziwą metodę (efekt uboczny!), doReturn().when() nie")
    void whenCallsRealMethodDoReturnDoesNot() {
        when(audit.record("zapis-przez-when")).thenReturn(100);          // prawdziwe record() już się wykonało!
        doReturn(100).when(audit).record("zapis-przez-doReturn");        // tu prawdziwa metoda NIE jest wołana

        assertThat(audit.events()).contains("zapis-przez-when").doesNotContain("zapis-przez-doReturn");
        assertThat(audit.record("zapis-przez-doReturn")).as("od teraz metoda zwraca ustaloną wartość").isEqualTo(100);
        assertThat(audit.events()).as("a prawdziwa nadal się nie wykonuje").doesNotContain("zapis-przez-doReturn");
    }

    @Test
    @DisplayName("Sekcja 2: szpieg jest szpiegiem Mockito, ale serwis dostał TEGO SAMEGO szpiega (to ten bean z kontekstu)")
    void serviceUsesTheSpy() {
        assertThat(Mockito.mockingDetails(audit).isSpy()).isTrue();

        service.create(new ProductRequest("Kubek", new BigDecimal("9.99")));

        verify(audit).record(anyString());   // wywołał go serwis, nie test — więc serwis ma w środku szpiega
    }
}
