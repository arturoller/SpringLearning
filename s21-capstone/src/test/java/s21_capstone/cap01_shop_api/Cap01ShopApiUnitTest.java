package s21_capstone.cap01_shop_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import s21_capstone.cap01_shop_api.common.ConflictException;
import s21_capstone.cap01_shop_api.customer.Customer;
import s21_capstone.cap01_shop_api.order.OrderStatus;
import s21_capstone.cap01_shop_api.order.ShopOrder;
import s21_capstone.cap01_shop_api.product.Category;
import s21_capstone.cap01_shop_api.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy JEDNOSTKOWE domeny projektu Cap01ShopApi — bez Springa, bez bazy, w milisekundach. Reguły biznesowe siedzą
 * w zwykłych klasach (enum OrderStatus, encja ShopOrder), więc najszybciej i najdokładniej sprawdza się je właśnie tu.
 * Encje tworzymy konstruktorem jak zwykłe obiekty — JPA nie jest do tego potrzebne.
 */
class Cap01ShopApiUnitTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 10, 0);

    static Product product(String sku, String price) {
        return new Product(sku, "Produkt " + sku, Category.DOM, new BigDecimal(price), 10);
    }

    static ShopOrder newOrder() {
        return new ShopOrder(new Customer("Jan Kowalski", "Warszawa", "jan@example.com", true), NOW);
    }

    @ParameterizedTest(name = "{0} → {1}: {2}")
    @CsvSource({
            "NOWE, OPLACONE, true", "NOWE, ANULOWANE, true", "NOWE, WYSLANE, false",
            "OPLACONE, WYSLANE, true", "OPLACONE, ANULOWANE, true", "OPLACONE, NOWE, false",
            "WYSLANE, DOSTARCZONE, true", "WYSLANE, ANULOWANE, false",
            "DOSTARCZONE, ANULOWANE, false", "ANULOWANE, NOWE, false", "NOWE, NOWE, false"})
    @DisplayName("4. Maszyna stanów: dozwolone i zabronione przejścia statusu")
    void statusTransitions(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canChangeTo(to)).isEqualTo(allowed);
    }

    @Test
    @DisplayName("4. Stany końcowe: z DOSTARCZONE i ANULOWANE nie ma wyjścia")
    void terminalStates() {
        assertThat(OrderStatus.DOSTARCZONE.allowedNext()).isEmpty();
        assertThat(OrderStatus.ANULOWANE.allowedNext()).isEmpty();
    }

    @Test
    @DisplayName("3. Suma zamówienia przeliczana przy każdej pozycji: 2 × 349.90 + 64.99 = 764.79")
    void totalIsRecalculated() {
        ShopOrder order = newOrder();
        order.addLine(product("ELE-003", "349.90"), 2);
        assertThat(order.getTotal()).isEqualByComparingTo("699.80");

        order.addLine(product("SPO-001", "64.99"), 1);
        assertThat(order.getTotal()).isEqualTo(new BigDecimal("764.79"));   // isEqualTo: także skala 2 (grosze)
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NOWE);
    }

    @Test
    @DisplayName("3. Pozycja kopiuje cenę: późniejsza zmiana ceny produktu nie zmienia zamówienia")
    void lineKeepsPriceFromOrderTime() {
        Product laptop = product("ELE-001", "5499.99");
        ShopOrder order = newOrder();
        order.addLine(laptop, 1);

        laptop.update(laptop.getName(), laptop.getCategory(), new BigDecimal("4999.99"), laptop.getStock());

        assertThat(order.getLines().getFirst().getUnitPrice()).isEqualByComparingTo("5499.99");
        assertThat(order.getTotal()).isEqualByComparingTo("5499.99");
    }

    @Test
    @DisplayName("4. Niedozwolone przejście → ConflictException z polami dla klienta (currentStatus, allowed)")
    void illegalStatusChange() {
        ShopOrder order = newOrder();
        order.changeStatus(OrderStatus.OPLACONE);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.DOSTARCZONE))
                .isInstanceOf(ConflictException.class)
                .satisfies(e -> {
                    ConflictException conflict = (ConflictException) e;
                    assertThat(conflict.code()).isEqualTo("ORDER_STATUS_CHANGE");
                    assertThat(conflict.details()).containsEntry("currentStatus", "OPLACONE")
                            .containsEntry("allowed", List.of("ANULOWANE", "WYSLANE"));
                });
        assertThat(order.getStatus()).as("status bez zmian").isEqualTo(OrderStatus.OPLACONE);
    }

    @Test
    @DisplayName("3. Pozycje tylko w zamówieniu NOWE; lista pozycji z zewnątrz jest tylko do odczytu")
    void linesAreProtected() {
        ShopOrder order = newOrder();
        order.addLine(product("KSI-001", "79.00"), 1);
        order.changeStatus(OrderStatus.OPLACONE);

        assertThatThrownBy(() -> order.addLine(product("KSI-002", "129.00"), 1))
                .isInstanceOf(ConflictException.class).hasMessage("Pozycje można zmieniać tylko w zamówieniu NOWE");
        assertThatThrownBy(() -> order.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> order.addLine(product("KSI-003", "99.00"), 0))
                .isInstanceOf(ConflictException.class);   // najpierw sprawdzany jest status (OPLACONE)
    }

    @Test
    @DisplayName("3. Encja pilnuje swoich reguł: ujemny stan i cena zero odrzucone, removeFromStock nie zejdzie poniżej 0")
    void productRules() {
        assertThatThrownBy(() -> new Product("DOM-009", "Czajnik", Category.DOM, BigDecimal.ZERO, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Cena musi być dodatnia: 0");
        Product coffee = product("SPO-001", "64.99");
        coffee.removeFromStock(10);
        assertThat(coffee.getStock()).isZero();
        assertThatThrownBy(() -> coffee.removeFromStock(1))
                .isInstanceOf(IllegalStateException.class).hasMessage("Za mało towaru SPO-001: jest 0, potrzeba 1");
    }
}
