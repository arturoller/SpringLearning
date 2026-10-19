package s21_capstone.solutions.cap01_shop_api;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s21_capstone.cap01_shop_api.Cap01Application;
import s21_capstone.cap01_shop_api.Cap01Config;
import s21_capstone.cap01_shop_api.Cap01Exercises.CategorySales;
import s21_capstone.cap01_shop_api.common.ConflictException;
import s21_capstone.cap01_shop_api.common.NotFoundException;
import s21_capstone.cap01_shop_api.order.OrderDtos.CreateOrderRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.LineRequest;
import s21_capstone.cap01_shop_api.order.OrderDtos.OrderResponse;
import s21_capstone.cap01_shop_api.order.OrderRepository;
import s21_capstone.cap01_shop_api.product.ProductRepository;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprawdzenia z Cap01ShopApiExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze.
 * Klasa rozwiązań leży poza skanowanym pakietem projektu, więc rejestrujemy ją wprost w {@code classes}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = {Cap01Application.class, Cap01ShopApiSolutions.class},
        properties = {"spring.datasource.url=jdbc:h2:mem:cap01-wzorce", "spring.flyway.clean-disabled=false"})
@ActiveProfiles("cap01")
class Cap01ShopApiSolutionsTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(LocalDateTime.of(2026, 10, 19, 10, 0).atZone(Cap01Config.SHOP_ZONE).toInstant(),
                    Cap01Config.SHOP_ZONE);
        }
    }

    @Autowired
    Cap01ShopApiSolutions solutions;

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ProductRepository products;

    @Autowired
    OrderRepository orders;

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    int stock(String sku) {
        return products.findBySku(sku).orElseThrow().getStock();
    }

    static CreateOrderRequest order(LineRequest... lines) {
        return new CreateOrderRequest(1L, List.of(lines));
    }

    @Test
    @DisplayName("Wzorzec 1: rabat z kodu (wielkość liter bez znaczenia), ważny w dniu validUntil, przeterminowany → 409")
    void solution1() {
        assertThat(solutions.totalWithDiscount(1, "JESIEN10")).isEqualByComparingTo("688.31");
        assertThat(solutions.totalWithDiscount(3, " vip15 ")).isEqualByComparingTo("4674.99");

        jdbc.sql("insert into discount_codes (code, percent, valid_until) values ('DZIS5', 5, date '2026-10-19')").update();
        assertThat(solutions.totalWithDiscount(1, "DZIS5")).as("ostatni dzień ważności").isEqualByComparingTo("726.55");

        assertThatThrownBy(() -> solutions.totalWithDiscount(1, "WIOSNA20"))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("DISCOUNT_EXPIRED"));
        assertThatThrownBy(() -> solutions.totalWithDiscount(1, "BRAK")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> solutions.totalWithDiscount(99, "JESIEN10")).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: raport po kategoriach — cena z zamówienia, bez anulowanych, przychód malejąco")
    void solution2() {
        List<CategorySales> report = solutions.salesByCategory();

        assertThat(report)
                .extracting(row -> row.category() + " " + row.pieces() + " "
                        + row.revenue().setScale(2, RoundingMode.UNNECESSARY).toPlainString())
                .containsExactly("ELEKTRONIKA 3 6199.79", "DOM 1 1899.00", "KSIAZKI 2 208.00", "SPOZYWCZE 3 194.97");
    }

    @Test
    @DisplayName("Wzorzec 3: brak towaru → 409 OUT_OF_STOCK i NIC się nie zmienia; udane zamówienie zdejmuje towar")
    void solution3() {
        long ordersBefore = orders.count();
        assertThatThrownBy(() -> solutions.placeWithStockCheck(order(
                new LineRequest("ELE-003", 2), new LineRequest("SPO-003", 1))))
                .isInstanceOfSatisfying(ConflictException.class, e -> {
                    assertThat(e.code()).isEqualTo("OUT_OF_STOCK");
                    assertThat(e.details()).containsEntry("sku", "SPO-003").containsEntry("available", 0)
                            .containsEntry("requested", 1);
                });
        assertThat(stock("ELE-003")).as("stan pierwszej pozycji wycofany (rollback)").isEqualTo(25);
        assertThat(orders.count()).as("żadnego nowego zamówienia").isEqualTo(ordersBefore);

        OrderResponse placed = solutions.placeWithStockCheck(order(new LineRequest("ELE-004", 4)));
        assertThat(placed.total()).isEqualByComparingTo("5196.00");
        assertThat(stock("ELE-004")).isZero();

        assertThatThrownBy(() -> solutions.placeWithStockCheck(order(new LineRequest("ELE-004", 1))))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.details()).containsEntry("available", 0));
        assertThatThrownBy(() -> solutions.placeWithStockCheck(order(new LineRequest("XXX-000", 1))))
                .isInstanceOf(NotFoundException.class);
    }
}
