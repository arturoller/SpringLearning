package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockingDetails;

/**
 * Sekcja 5 lekcji Test02MockitoBeans: zerowanie (reset) atrap między testami. Atrapa z {@code @MockitoBean} żyje
 * w kontekście, a kontekst jest współdzielony (pamięć podręczna) — więc Spring PO KAŻDYM teście zeruje jej
 * nauczenie i zapis wywołań ({@code reset = MockReset.AFTER} — wartość domyślna).
 *
 * <p>Kolejność metod wymuszamy {@code @Order} tylko po to, żeby POKAZAĆ, że test 2 nie widzi śladów testu 1.
 * Test 2 uruchomiony osobno też przechodzi — i właśnie o to chodzi: żaden test nie zależy od innego.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)   // method orderer = porządkowanie metod testowych
class Test02MockitoBeansResetTest {

    @MockitoBean
    OrderNotifier notifier;

    @Autowired
    OrderService service;

    @Test
    @Order(1)
    @DisplayName("5. Test 1 uczy atrapę rzucać wyjątek („serwer poczty nie działa”)")
    void firstTestStubsException() {
        willThrow(new IllegalStateException("Serwer poczty nie działa")).given(notifier).orderPlaced(any());

        assertThatThrownBy(() -> service.placeOrder("jan@example.com", "SPO-001", 1))
                .hasMessage("Serwer poczty nie działa");
    }

    @Test
    @Order(2)
    @DisplayName("5. Test 2 dostaje WYZEROWANĄ atrapę: brak wyjątku i tylko jego własne wywołanie w zapisie")
    void secondTestSeesFreshMock() {
        service.placeOrder("jan@example.com", "SPO-001", 1);   // nie rzuca — nauczenie z testu 1 wyzerowane

        assertThat(mockingDetails(notifier).getInvocations()).hasSize(1);
    }
}
