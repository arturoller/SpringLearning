package s10_transactions.solutions.tx04_read_only_isolation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s10_transactions.tx04_read_only_isolation.SecondUser;
import s10_transactions.tx04_read_only_isolation.Tx04Application;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Tx04ReadOnlyIsolationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Klasa rozwiązań leży poza skanowanym pakietem lekcji, więc rejestrujemy ją wprost w {@code classes}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = {Tx04Application.class, Tx04ReadOnlyIsolationSolutions.class})
@ActiveProfiles("tx04")
class Tx04ReadOnlyIsolationSolutionsTest {

    @Autowired
    Tx04ReadOnlyIsolationSolutions solutions;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void resetProducts() {
        jdbc.sql("update products set stock = 7 where sku = 'ELE-001'").update();
        jdbc.sql("update products set stock = 2 where sku = 'DOM-001'").update();
        jdbc.sql("update versioned_products set stock = 7, version = 0 where sku = 'ELE-001'").update();
    }

    int stock(String sku) {
        return jdbc.sql("select stock from products where sku = ?").param(sku).query(Integer.class).single();
    }

    @Test
    @DisplayName("Wzorzec 1: dwa odczyty w jednej transakcji dają ten sam wynik mimo cudzego commitu")
    void solution1() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            assertThat(solutions.readStockTwiceStable("ELE-001", () -> bob.updateAndCommit(
                    "update products set stock = stock - 1 where sku = ?", "ELE-001")))
                    .containsExactly(7, 7);
        }
        assertThat(stock("ELE-001")).as("sprzedaż drugiego użytkownika jest w bazie").isEqualTo(6);
    }

    @Test
    @DisplayName("Wzorzec 2: sprzedaż bez utraconej aktualizacji i bez zejścia poniżej zera")
    void solution2() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            assertThat(solutions.sellWithPauseSafely("ELE-001", 2, () -> bob.updateAndCommit(
                    "update products set stock = stock - 1 where sku = ?", "ELE-001"))).isTrue();
            assertThat(stock("ELE-001")).as("7 - 1 (drugi użytkownik) - 2 (my)").isEqualTo(4);

            assertThat(solutions.sellWithPauseSafely("DOM-001", 2, () -> bob.updateAndCommit(
                    "update products set stock = stock - 1 where sku = ?", "DOM-001"))).as("została 1 szt.").isFalse();
            assertThat(stock("DOM-001")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Wzorzec 3: konflikt @Version w 1. próbie → 2. próba w nowej transakcji się udaje")
    void solution3() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            int attempts = solutions.sellVersionedWithRetry("ELE-001", 2, () -> bob.updateAndCommit(
                    "update versioned_products set stock = stock - 1, version = version + 1 where sku = ?", "ELE-001"));
            assertThat(attempts).isEqualTo(2);
        }
        assertThat(jdbc.sql("select stock || '/' || version from versioned_products where sku = 'ELE-001'")
                .query(String.class).single()).as("stan/wersja: 7 - 1 - 2, dwie zmiany").isEqualTo("4/2");

        assertThat(solutions.sellVersionedWithRetry("ELE-001", 1, () -> { })).as("bez konfliktu — 1 próba").isEqualTo(1);
    }
}
