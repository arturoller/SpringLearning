package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sekcje 1–2 lekcji Err05CustomExceptions: hierarchia wyjątków — zwykłe testy jednostkowe (bez Springa i bez HTTP). */
class Err05CustomExceptionsHierarchyTest {

    @Test
    @DisplayName("1. Wszystkie wyjątki sklepu to ShopException i RuntimeException; każdy ma swój kod błędu")
    void hierarchy() {
        assertThat(new ProductNotFoundException(7)).isInstanceOf(ShopException.class).isInstanceOf(RuntimeException.class);
        assertThat(new OutOfStockException(7, 3, 1)).isInstanceOf(ShopException.class);
        assertThat(new PaymentGatewayException(new RuntimeException("x"))).isInstanceOf(ShopException.class);

        assertThat(new ProductNotFoundException(7).getCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        assertThat(new OutOfStockException(7, 3, 1).getCode()).isEqualTo(ErrorCode.OUT_OF_STOCK);
        assertThat(new PaymentGatewayException(new RuntimeException("x")).getCode()).isEqualTo(ErrorCode.PAYMENT_UNAVAILABLE);
    }

    @Test
    @DisplayName("1. Jeden catch (ShopException) łapie wszystkie wyjątki domenowe, a inne wyjątki przepuszcza")
    void singleCatchForWholeFamily() {
        assertThatThrownBy(() -> { throw new OutOfStockException(1, 9, 2); }).isInstanceOf(ShopException.class);

        boolean caught = false;
        try {
            throw new ProductNotFoundException(1);
        } catch (ShopException e) {
            caught = true;
        }
        assertThat(caught).isTrue();
    }

    @Test
    @DisplayName("2. Wyjątki niosą dane jako pola (nie tylko tekst), a komunikat jest napisany dla klienta")
    void carriesData() {
        OutOfStockException e = new OutOfStockException(2, 3, 0);

        assertThat(e.getProductId()).isEqualTo(2);
        assertThat(e.getRequested()).isEqualTo(3);
        assertThat(e.getAvailable()).isZero();
        assertThat(e).hasMessage("Za mało towaru (produkt 2): zamówiono 3, dostępne 0");
    }

    @Test
    @DisplayName("2. PaymentGatewayException opakowuje przyczynę techniczną: w komunikacie jej nie ma, w cause jest")
    void wrapsCause() {
        IllegalStateException internal = new IllegalStateException("Connection refused: pay.internal:8443");
        PaymentGatewayException e = new PaymentGatewayException(internal);

        assertThat(e.getCause()).isSameAs(internal);
        assertThat(e.getMessage()).doesNotContain("pay.internal");
    }
}
