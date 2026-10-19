package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * Testy ĆWICZEŃ lekcji Test02MockitoBeans. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Uzupełnij miejsca z TODO i usuń wywołania {@code todo()}. Asercje na końcu metod są gotowe — nie zmieniaj ich.
 */
@Tag("cwiczenie")
class Test02MockitoBeansExercisesTest {

    /** Znacznik „tu jeszcze nic nie ma” — rzuca wyjątek, dopóki go nie usuniesz. */
    static <T> T todo() {
        throw new UnsupportedOperationException("TODO");
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Exercise1 {

        @Mock
        ProductCatalog catalog;

        @Mock
        OrderNotifier notifier;

        @InjectMocks
        OrderService service;

        @Test
        @DisplayName("Ćwiczenie 1: atrapa katalogu zwraca „Java. Podstawy” (12900 gr), serwis liczy 2 sztuki")
        void exercise1() {
            // TODO: naucz atrapę catalog: getBySku("KSI-002") → new Product("KSI-002", "Java. Podstawy", 12900)
            todo();

            assertThat(service.placeOrder("maria.nowak@example.com", "KSI-002", 2).totalGrosze()).isEqualTo(25800);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Exercise2 {

        @Mock
        ProductCatalog catalog;

        @Mock
        OrderNotifier notifier;

        @InjectMocks
        OrderService service;

        @Captor
        ArgumentCaptor<OrderConfirmation> captor;

        @Test
        @DisplayName("Ćwiczenie 2: złap ArgumentCaptor-em potwierdzenie przekazane do powiadamiacza")
        void exercise2() {
            when(catalog.getBySku("SPO-002")).thenReturn(new Product("SPO-002", "Czekolada gorzka", 749));
            service.placeOrder("marek@example.com", "SPO-002", 10);

            // TODO: sprawdź (verify), że notifier.orderPlaced(...) wywołano, złap argument w captor i weź jego wartość
            OrderConfirmation sent = todo();

            assertThat(sent.customerEmail()).isEqualTo("marek@example.com");
            assertThat(sent.totalGrosze()).isEqualTo(7490);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Exercise3 {

        @Mock
        ProductCatalog catalog;

        @Mock
        OrderNotifier notifier;

        @InjectMocks
        OrderService service;

        @Test
        @DisplayName("Ćwiczenie 3: atrapa rzuca „Nieznany produkt”, a powiadomienie nie wychodzi")
        void exercise3() {
            // TODO: naucz atrapę catalog, że getBySku("ODZ-003") rzuca NoSuchElementException("Nieznany produkt: ODZ-003")
            todo();

            assertThatThrownBy(() -> service.placeOrder("ola.pawlak@example.com", "ODZ-003", 1))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Nieznany produkt: ODZ-003");
            assertThat(mockingDetails(notifier).getInvocations()).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    class Exercise4 {

        @MockitoSpyBean
        ProductCatalog catalog;

        @MockitoBean
        OrderNotifier notifier;

        @Autowired
        OrderService service;

        @Test
        @DisplayName("Ćwiczenie 4: szpieg katalogu zna nowy produkt DOM-003, a stare dane zostają prawdziwe")
        void exercise4() {
            // TODO: naucz SZPIEGA catalog: getBySku("DOM-003") → new Product("DOM-003", "Odkurzacz", 59900)
            //       (uwaga na pułapkę z sekcji 3!)
            todo();

            assertThat(service.placeOrder("jan@example.com", "DOM-003", 2).totalGrosze()).isEqualTo(119800);
            assertThat(catalog.getBySku("SPO-001").priceGrosze()).isEqualTo(6499);
        }
    }
}
