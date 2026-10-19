package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sekcja 3 lekcji Test02MockitoBeans: {@code @MockitoSpyBean} — szpieg (spy) owinięty wokół PRAWDZIWEGO beana.
 * Wywołania idą do prawdziwego katalogu, ale możesz je sprawdzać ({@code verify}) i podmienić pojedyncze odpowiedzi.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class Test02MockitoBeansSpyBeanTest {

    @MockitoSpyBean   // spy bean = bean-szpieg: prawdziwy obiekt + nagrywanie wywołań
    ProductCatalog catalog;

    @MockitoBean
    OrderNotifier notifier;

    @Autowired
    OrderService service;

    @Test
    @DisplayName("3. Szpieg: dane z PRAWDZIWEGO katalogu (4 × 749 = 2996) i sprawdzenie wywołania")
    void spyCallsRealMethod() {
        assertThat(mockingDetails(catalog).isSpy()).isTrue();

        OrderConfirmation confirmation = service.placeOrder("ewa.lis@example.com", "SPO-002", 4);

        assertThat(confirmation.totalGrosze()).isEqualTo(2996);
        verify(catalog).getBySku("SPO-002");
    }

    @Test
    @DisplayName("3. Szpieg: doReturn(...).when(spy) podmienia JEDNĄ odpowiedź, reszta zostaje prawdziwa")
    void doReturnOnSpy() {
        doReturn(new Product("DOM-003", "Odkurzacz", 59900)).when(catalog).getBySku("DOM-003");

        assertThat(service.placeOrder("jan@example.com", "DOM-003", 1).totalGrosze()).isEqualTo(59900);
        assertThat(catalog.getBySku("SPO-001").name()).isEqualTo("Kawa ziarnista 1kg");   // prawdziwe dane
    }

    @Test
    @DisplayName("3. PUŁAPKA: when(spy.getBySku(...)) WYWOŁUJE prawdziwą metodę — a ona rzuca wyjątek")
    void whenOnSpyCallsRealMethod() {
        assertThatThrownBy(() -> when(catalog.getBySku("DOM-003"))
                .thenReturn(new Product("DOM-003", "Odkurzacz", 59900)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Nieznany produkt: DOM-003");
    }
}
