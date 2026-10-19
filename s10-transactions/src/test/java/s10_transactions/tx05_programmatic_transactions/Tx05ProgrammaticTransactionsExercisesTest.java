package s10_transactions.tx05_programmatic_transactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Tx05ProgrammaticTransactions. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("tx05")
class Tx05ProgrammaticTransactionsExercisesTest {

    @Autowired
    Tx05Exercises exercises;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void resetCatalog() {
        jdbc.sql("update products set price = 5499.99 where sku = 'ELE-001'").update();
        jdbc.sql("update products set price = 349.90 where sku = 'ELE-003'").update();
        jdbc.sql("update products set price = 64.99 where sku = 'SPO-001'").update();
        jdbc.sql("update products set price = 79.00 where sku = 'KSI-001'").update();
        jdbc.sql("update products set price = 129.00 where sku = 'KSI-002'").update();
    }

    BigDecimal price(String sku) {
        return jdbc.sql("select price from products where sku = ?").param(sku).query(BigDecimal.class).single();
    }

    @Test
    @DisplayName("Ćwiczenie 1: podwyżka o procent w TransactionTemplate.execute")
    void exercise1() {
        assertThat(exercises.raisePriceByPercent("KSI-001", 10)).isEqualByComparingTo("86.90");
        assertThat(price("KSI-001")).isEqualByComparingTo("86.90");
        assertThat(exercises.raisePriceByPercent("ELE-003", 5)).as("367.395 → 367.40").isEqualByComparingTo("367.40");

        assertThatThrownBy(() -> exercises.raisePriceByPercent("XXX-999", 10)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: suma cennika po obniżce 10% liczona na sucho — bez zmian w bazie")
    void exercise2() {
        // 4949.99 + 314.91 + 58.49 + 71.10 + 116.10
        assertThat(exercises.previewCatalogValueAfterDiscount(10)).isEqualByComparingTo("5510.59");
        assertThat(price("ELE-001")).isEqualByComparingTo("5499.99");
        assertThat(price("KSI-002")).isEqualByComparingTo("129.00");
    }

    @Test
    @DisplayName("Ćwiczenie 3: import paczkami po 2 — zła paczka wycofana w całości, reszta zapisana")
    void exercise3() {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        prices.put("ELE-001", new BigDecimal("5299.99"));     // paczka 1
        prices.put("ELE-003", new BigDecimal("299.90"));      // paczka 1
        prices.put("SPO-001", new BigDecimal("69.99"));       // paczka 2 — dobra cena, ale…
        prices.put("KSI-001", new BigDecimal("0.00"));        // paczka 2 — …zła cena w tej samej paczce
        prices.put("KSI-002", new BigDecimal("119.00"));      // paczka 3

        assertThat(exercises.importInChunks(prices, 2)).isEqualTo(3);
        assertThat(price("ELE-001")).isEqualByComparingTo("5299.99");
        assertThat(price("ELE-003")).isEqualByComparingTo("299.90");
        assertThat(price("SPO-001")).as("wycofana razem ze swoją paczką").isEqualByComparingTo("64.99");
        assertThat(price("KSI-001")).isEqualByComparingTo("79.00");
        assertThat(price("KSI-002")).isEqualByComparingTo("119.00");
    }

    @Test
    @DisplayName("Ćwiczenie 4: ręczna transakcja — wszystko albo nic i prawdziwa przyczyna błędu")
    void exercise4() {
        Map<String, BigDecimal> good = new LinkedHashMap<>();
        good.put("ELE-003", new BigDecimal("329.90"));
        good.put("KSI-002", new BigDecimal("119.00"));
        exercises.changePricesManually(good);
        assertThat(price("ELE-003")).isEqualByComparingTo("329.90");
        assertThat(price("KSI-002")).isEqualByComparingTo("119.00");

        Map<String, BigDecimal> badPrice = new LinkedHashMap<>();
        badPrice.put("ELE-001", new BigDecimal("5199.99"));
        badPrice.put("SPO-001", new BigDecimal("0.00"));
        assertThatThrownBy(() -> exercises.changePricesManually(badPrice))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(price("ELE-001")).isEqualByComparingTo("5499.99");

        Map<String, BigDecimal> unknown = new LinkedHashMap<>();
        unknown.put("KSI-001", new BigDecimal("70.00"));
        unknown.put("XXX-999", new BigDecimal("1.00"));
        assertThatThrownBy(() -> exercises.changePricesManually(unknown)).isInstanceOf(NoSuchElementException.class);
        assertThat(price("KSI-001")).isEqualByComparingTo("79.00");

        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .as("żadna transakcja nie została przy wątku").isFalse();
    }
}
