package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Sekcja 1 lekcji Test02MockitoBeans: czysty Mockito w teście jednostkowym — BEZ Springa. Żadnego kontekstu,
 * żadnych beanów: Mockito tworzy atrapy, a {@code @InjectMocks} buduje prawdziwy OrderService i podaje mu je
 * do konstruktora. Start: milisekundy.
 */
@ExtendWith(MockitoExtension.class)   // MockitoExtension = rozszerzenie JUnit, które przed KAŻDYM testem tworzy świeże atrapy
class Test02MockitoBeansUnitTest {

    @Mock   // mock = atrapa: obiekt, który nic nie robi, dopóki go nie „nauczysz”
    ProductCatalog catalog;

    @Mock
    OrderNotifier notifier;

    @InjectMocks   // inject mocks = wstrzyknij atrapy: new OrderService(catalog, notifier)
    OrderService service;

    @Captor   // captor = „łapacz” argumentu, z którym wywołano atrapę
    ArgumentCaptor<OrderConfirmation> sent;

    @Test
    @DisplayName("1. when/thenReturn: atrapa katalogu podaje cenę, serwis liczy kwotę (2 × 6499 = 12998)")
    void stubbing() {
        when(catalog.getBySku("SPO-001")).thenReturn(new Product("SPO-001", "Kawa ziarnista 1kg", 6499));

        OrderConfirmation confirmation = service.placeOrder("jan@example.com", "SPO-001", 2);

        assertThat(confirmation.totalGrosze()).isEqualTo(12998);
    }

    @Test
    @DisplayName("1. verify + ArgumentCaptor: serwis powiadomił DOKŁADNIE raz i przekazał poprawne potwierdzenie")
    void verifyWithCaptor() {
        when(catalog.getBySku("KSI-001")).thenReturn(new Product("KSI-001", "Czysty kod", 7900));

        service.placeOrder("adam.mazur@example.com", "KSI-001", 3);

        verify(notifier).orderPlaced(sent.capture());   // verify = sprawdź, że wywołano (domyślnie: dokładnie raz)
        assertThat(sent.getValue())
                .isEqualTo(new OrderConfirmation("adam.mazur@example.com", "KSI-001", 3, 23700));
        verifyNoMoreInteractions(notifier);             // i nic więcej
    }

    @Test
    @DisplayName("1. Zła ilość: wyjątek, a katalog i powiadamiacz nietknięte (verifyNoInteractions)")
    void invalidQuantity() {
        assertThatThrownBy(() -> service.placeOrder("jan@example.com", "SPO-001", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ilość musi być dodatnia, a jest: 0");

        verifyNoInteractions(catalog, notifier);
    }

    @Test
    @DisplayName("1. thenThrow: atrapa rzuca wyjątek, serwis go przepuszcza, powiadomienia NIE ma (never)")
    void stubbedException() {
        when(catalog.getBySku("XXX-999")).thenThrow(new NoSuchElementException("Nieznany produkt: XXX-999"));

        assertThatThrownBy(() -> service.placeOrder("jan@example.com", "XXX-999", 1))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Nieznany produkt: XXX-999");
        verify(notifier, never()).orderPlaced(any());
    }

    @Test
    @DisplayName("1. PUŁAPKA: nienauczona atrapa zwraca null → NullPointerException w środku serwisu")
    void unstubbedMockReturnsNull() {
        assertThat(catalog.getBySku("SPO-001")).as("atrapa bez nauczenia zwraca wartość domyślną").isNull();

        assertThatThrownBy(() -> service.placeOrder("jan@example.com", "SPO-001", 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Cannot invoke \"s11_testing.test02_mockito_beans.Product.priceGrosze()\" "
                        + "because \"product\" is null");
    }
}
