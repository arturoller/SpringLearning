package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1, 2 i 5 lekcji Err05CustomExceptions — zwykły test jednostkowy, BEZ Springa i bez HTTP: serwis tworzymy przez
 * {@code new OrderService()}. To dowód, że warstwa serwisu nie zna HTTP; sprawdzamy typ i kod wyjątku, nie status.
 */
class Err05OrderServiceTest {

    private final OrderService service = new OrderService();

    @Test
    @DisplayName("Sekcja 1: hierarchia — wyjątki sklepu są niekontrolowane (RuntimeException) i mają wspólnego przodka ShopException")
    void hierarchy() {
        assertThat(new ProductNotFoundException(9))
                .isInstanceOf(NotFoundException.class).isInstanceOf(ShopException.class).isInstanceOf(RuntimeException.class);
        assertThat(new OrderNotFoundException(9)).isInstanceOf(NotFoundException.class);
        assertThat(new OutOfStockException("Kawa", 9, 5)).isInstanceOf(BusinessRuleException.class).isInstanceOf(ShopException.class);
        assertThat(new OrderAlreadyShippedException(2)).isInstanceOf(BusinessRuleException.class);
        assertThat(new MinimumOrderValueException(2)).isInstanceOf(BusinessRuleException.class);
        assertThat(new ProductNotFoundException(9)).isNotInstanceOf(BusinessRuleException.class);   // dwie osobne gałęzie
    }

    @Test
    @DisplayName("Sekcja 2: serwis rzuca wyjątki domenowe z kodem biznesowym i czytelnym komunikatem")
    void serviceThrowsDomainExceptions() {
        assertThatThrownBy(() -> service.getProduct(9))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Nie ma produktu o id 9")
                .extracting(e -> ((ShopException) e).getCode()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThatThrownBy(() -> service.getOrder(7)).isInstanceOf(OrderNotFoundException.class);
        assertThatThrownBy(() -> service.placeOrder(1, 9))
                .isInstanceOf(OutOfStockException.class)
                .hasMessage("Brak towaru: Kawa (chcesz 9, jest 5)");
        assertThatThrownBy(() -> service.placeOrder(2, 2)).isInstanceOf(OutOfStockException.class);      // Herbata: 0 sztuk
        assertThatThrownBy(() -> service.placeOrder(1, 1)).isInstanceOf(MinimumOrderValueException.class);
        assertThatThrownBy(() -> service.ship(2)).isInstanceOf(OrderAlreadyShippedException.class)
                .extracting(e -> ((ShopException) e).getCode()).isEqualTo("ORDER_ALREADY_SHIPPED");
    }

    @Test
    @DisplayName("Sekcja 2: poprawne działania nie rzucają wyjątków, a stan magazynu się zmienia")
    void happyPath() {
        Order order = service.placeOrder(1, 2);

        assertThat(order.shipped()).isFalse();
        assertThat(service.getProduct(1).stock()).isEqualTo(3);
        assertThat(service.ship(order.id()).shipped()).isTrue();
    }

    @Test
    @DisplayName("Sekcja 5 (PUŁAPKA): 'czy istnieje?' to Optional, nie wyjątek — brak produktu w pytaniu nie jest błędem")
    void optionalForQuestions() {
        assertThat(service.findProduct(1)).isPresent();
        assertThat(service.findProduct(9)).isEmpty();                       // zwykła odpowiedź, bez rzucania i łapania
        assertThatThrownBy(() -> service.getProduct(9)).isInstanceOf(ProductNotFoundException.class);   // "wymagam" = wyjątek
    }
}
