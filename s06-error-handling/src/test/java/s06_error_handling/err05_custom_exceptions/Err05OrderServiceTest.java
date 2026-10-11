package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sekcja 3: serwis rzuca wyjątki domenowe i nic nie wie o HTTP — testujemy go bez Springa. */
class Err05OrderServiceTest {

    private final OrderService service = new OrderService();

    @Test
    @DisplayName("3. Poprawne zamówienie → potwierdzenie z kolejnymi numerami")
    void placesOrder() {
        OrderConfirmation first = service.place(new OrderRequest("Ala", 1, 2));
        OrderConfirmation second = service.place(new OrderRequest("Ala", 1, 1));

        assertThat(first.quantity()).isEqualTo(2);
        assertThat(second.orderId()).isEqualTo(first.orderId() + 1);
    }

    @Test
    @DisplayName("3. Brak produktu → ProductNotFoundException, za duża ilość → OutOfStockException z liczbami")
    void throwsDomainExceptions() {
        assertThatThrownBy(() -> service.place(new OrderRequest("Ala", 999, 1)))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.place(new OrderRequest("Ala", 1, 6)))
                .isInstanceOfSatisfying(OutOfStockException.class, e -> {
                    assertThat(e.getRequested()).isEqualTo(6);
                    assertThat(e.getAvailable()).isEqualTo(5);
                });
    }
}
