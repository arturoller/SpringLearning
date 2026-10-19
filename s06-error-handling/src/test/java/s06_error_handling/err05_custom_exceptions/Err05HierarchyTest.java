package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Zwykłe testy jednostkowe (bez Springa): kształt hierarchii wyjątków i łańcuch przyczyn. Spring nie jest tu potrzebny. */
class Err05HierarchyTest {

    private final OrderService service = new OrderService();

    @Test
    @DisplayName("1. ShopException jest wyjątkiem niekontrolowanym (RuntimeException) i zapieczętowanym z trzema podklasami")
    void sealedUncheckedHierarchy() {
        assertThat(RuntimeException.class).isAssignableFrom(ShopException.class);
        assertThat(ShopException.class.isSealed()).isTrue();
        assertThat(Arrays.stream(ShopException.class.getPermittedSubclasses()).map(Class::getSimpleName).sorted())
                .containsExactly("InvalidOrderException", "OutOfStockException", "ProductNotFoundException");
    }

    @Test
    @DisplayName("2. Awaria infrastruktury NIE jest błędem sklepu (nie dziedziczy po ShopException) — to nie wina klienta")
    void infrastructureIsNotShopException() {
        assertThat(ShopException.class.isAssignableFrom(StorageUnavailableException.class)).isFalse();
    }

    @Test
    @DisplayName("3. Wyjątek domeny nosi stabilny kod i dane (details) — niezależnie od HTTP")
    void codeAndDetails() {
        OutOfStockException e = new OutOfStockException("ELE-001", 7, 50);

        assertThat(e.code()).isEqualTo("BRAK_TOWARU");
        assertThat(e.details()).containsEntry("sku", "ELE-001").containsEntry("available", 7);
        assertThat(e).hasMessage("Brak towaru ELE-001: dostępne 7, żądane 50");
        assertThat(new InvalidOrderException("x").details()).isEmpty();
    }

    @Test
    @DisplayName("4. Opakowanie wyjątku niższej warstwy zachowuje przyczynę (cause)")
    void wrappingKeepsCause() {
        assertThatThrownBy(service::salesReport)
                .isInstanceOf(StorageUnavailableException.class)
                .hasMessage("Nie udało się odczytać raportu sprzedaży")
                .hasCauseInstanceOf(IllegalStateException.class)
                .cause().hasMessageStartingWith("Connection refused");
    }

    @Test
    @DisplayName("5. Serwis sprawdza reguły po kolei: pusta lista → InvalidOrder, nieznany SKU → InvalidOrder, za dużo → OutOfStock")
    void serviceRules() {
        assertThatThrownBy(() -> service.place(new NewOrder("Jan", java.util.List.of())))
                .isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> service.place(new NewOrder("Jan", java.util.List.of(new OrderLine("ZZZ-1", 1)))))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("ZZZ-1");
        assertThatThrownBy(() -> service.place(new NewOrder("Jan", java.util.List.of(new OrderLine("ELE-002", 1)))))
                .isInstanceOf(OutOfStockException.class);   // ELE-002 jest w magazynie 0 sztuk
        assertThat(service.place(new NewOrder("Jan", java.util.List.of(new OrderLine("ELE-001", 7))))).isEqualTo("ZAM-001");
    }

    @Test
    @DisplayName("6. PUŁAPKA: opakowanie z samym getMessage() i bez przyczyny gubi stos i pierwotny wyjątek")
    void wrappingWithoutCauseLosesIt() {
        IllegalStateException original = new IllegalStateException("Connection refused");

        StorageUnavailableException bad = new StorageUnavailableException(original.getMessage(), null);
        StorageUnavailableException good = new StorageUnavailableException("Raport niedostępny", original);

        assertThat(bad.getCause()).isNull();
        assertThat(good.getCause()).isSameAs(original);
    }
}
