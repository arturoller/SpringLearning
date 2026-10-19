package s20_modulith.mod01_modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import s20_modulith.mod01_modules.orders.OrderConfirmation;
import s20_modulith.mod01_modules.orders.OrderService;
import s20_modulith.mod01_modules.orders.internal.OrderNumbers;
import s20_modulith.mod01_modules.products.internal.ProductStore;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test sekcji 7 lekcji Mod01Modules: w czasie działania moduły to zwykłe beany Springa.
 *
 * <p>Wybór rodzaju testu: {@code @SpringBootTest}, bo sprawdzamy współpracę trzech modułów w prawdziwym kontekście
 * aplikacji Mod01Application (profil {@code mod01}: własna baza H2).</p>
 */
@SpringBootTest
@ActiveProfiles("mod01")
class Mod01ModulesSpringTest {

    @Autowired
    private OrderService orders;

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("7. Zamówienie przechodzi przez trzy moduły, a beany z pakietów internal istnieją jak każde inne")
    void section7() {
        OrderConfirmation confirmation = orders.placeOrder(1, "KSI-001", 2);
        assertThat(confirmation.orderId()).matches("ZAM-\\d{3}");
        assertThat(confirmation.customerName()).isEqualTo("Jan Kowalski");
        assertThat(confirmation.total()).isEqualByComparingTo(new BigDecimal("158.00"));   // 2 × 79.00

        assertThatThrownBy(() -> orders.placeOrder(1, "ELE-002", 1))                       // Smartfon X: stan 0
                .isInstanceOf(IllegalStateException.class).hasMessage("Za mało sztuk na stanie: ELE-002");
        assertThatThrownBy(() -> orders.placeOrder(99, "KSI-001", 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Nie ma klienta o id 99");

        // Spring tworzy beany także z pakietów internal — granice modułów to sprawa testu verify(), nie kontenera
        assertThat(context.getBeanNamesForType(ProductStore.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(OrderNumbers.class)).hasSize(1);
    }
}
