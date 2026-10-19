package s11_testing.solutions.test02_mockito_beans;

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
import s11_testing.test02_mockito_beans.OrderConfirmation;
import s11_testing.test02_mockito_beans.OrderNotifier;
import s11_testing.test02_mockito_beans.OrderService;
import s11_testing.test02_mockito_beans.Product;
import s11_testing.test02_mockito_beans.ProductCatalog;
import s11_testing.test02_mockito_beans.Test02Application;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Test02MockitoBeans (te same testy co w Test02MockitoBeansExercisesTest,
 * już uzupełnione). {@code @Tag("wzorzec")} — biegną zawsze. Zajrzyj dopiero po własnej próbie!
 * Test ze Springiem wskazuje klasę konfiguracji jawnie ({@code classes = Test02Application.class}), bo pakiet
 * {@code solutions…} nie leży pod pakietem lekcji.
 */
@Tag("wzorzec")
class Test02MockitoBeansSolutionsTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Solution1 {

        @Mock
        ProductCatalog catalog;

        @Mock
        OrderNotifier notifier;

        @InjectMocks
        OrderService service;

        @Test
        @DisplayName("Wzorzec 1: atrapa katalogu zwraca „Java. Podstawy” (12900 gr), serwis liczy 2 sztuki")
        void solution1() {
            when(catalog.getBySku("KSI-002")).thenReturn(new Product("KSI-002", "Java. Podstawy", 12900));

            assertThat(service.placeOrder("maria.nowak@example.com", "KSI-002", 2).totalGrosze()).isEqualTo(25800);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Solution2 {

        @Mock
        ProductCatalog catalog;

        @Mock
        OrderNotifier notifier;

        @InjectMocks
        OrderService service;

        @Captor
        ArgumentCaptor<OrderConfirmation> captor;

        @Test
        @DisplayName("Wzorzec 2: złap ArgumentCaptor-em potwierdzenie przekazane do powiadamiacza")
        void solution2() {
            when(catalog.getBySku("SPO-002")).thenReturn(new Product("SPO-002", "Czekolada gorzka", 749));
            service.placeOrder("marek@example.com", "SPO-002", 10);

            verify(notifier).orderPlaced(captor.capture());
            OrderConfirmation sent = captor.getValue();

            assertThat(sent.customerEmail()).isEqualTo("marek@example.com");
            assertThat(sent.totalGrosze()).isEqualTo(7490);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Solution3 {

        @Mock
        ProductCatalog catalog;

        @Mock
        OrderNotifier notifier;

        @InjectMocks
        OrderService service;

        @Test
        @DisplayName("Wzorzec 3: atrapa rzuca „Nieznany produkt”, a powiadomienie nie wychodzi")
        void solution3() {
            when(catalog.getBySku("ODZ-003")).thenThrow(new NoSuchElementException("Nieznany produkt: ODZ-003"));

            assertThatThrownBy(() -> service.placeOrder("ola.pawlak@example.com", "ODZ-003", 1))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Nieznany produkt: ODZ-003");
            assertThat(mockingDetails(notifier).getInvocations()).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(classes = Test02Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
    class Solution4 {

        @MockitoSpyBean
        ProductCatalog catalog;

        @MockitoBean
        OrderNotifier notifier;

        @Autowired
        OrderService service;

        @Test
        @DisplayName("Wzorzec 4: szpieg katalogu zna nowy produkt DOM-003, a stare dane zostają prawdziwe")
        void solution4() {
            // doReturn(...).when(szpieg) — NIE when(szpieg.getBySku(...)), bo to wywołałoby prawdziwą metodę (wyjątek).
            doReturn(new Product("DOM-003", "Odkurzacz", 59900)).when(catalog).getBySku("DOM-003");

            assertThat(service.placeOrder("jan@example.com", "DOM-003", 2).totalGrosze()).isEqualTo(119800);
            assertThat(catalog.getBySku("SPO-001").priceGrosze()).isEqualTo(6499);
        }
    }
}
