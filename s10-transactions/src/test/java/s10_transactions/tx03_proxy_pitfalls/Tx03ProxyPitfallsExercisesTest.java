package s10_transactions.tx03_proxy_pitfalls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Tx03ProxyPitfalls. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("tx03")
class Tx03ProxyPitfallsExercisesTest {

    @Autowired
    Tx03Exercises exercises;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void resetStock() {
        jdbc.sql("update stock_items set quantity = 120 where sku = 'SPO-001'").update();
        jdbc.sql("update stock_items set quantity = 300 where sku = 'SPO-002'").update();
        jdbc.sql("update stock_items set quantity = 0 where sku = 'SPO-003'").update();
    }

    int quantity(String sku) {
        return jdbc.sql("select quantity from stock_items where sku = ?").param(sku).query(Integer.class).single();
    }

    @Test
    @DisplayName("Ćwiczenie 1: dostawy wszystkie albo żadna")
    void exercise1() {
        Map<String, Integer> ok = new LinkedHashMap<>();
        ok.put("SPO-001", 10);
        ok.put("SPO-003", 4);
        exercises.restockAll(ok);
        assertThat(quantity("SPO-001")).isEqualTo(130);
        assertThat(quantity("SPO-003")).isEqualTo(4);

        Map<String, Integer> broken = new LinkedHashMap<>();
        broken.put("SPO-002", 50);
        broken.put("SPO-999", 1);
        assertThatThrownBy(() -> exercises.restockAll(broken)).isInstanceOf(NoSuchElementException.class);
        assertThat(quantity("SPO-002")).as("dostawa czekolady wycofana").isEqualTo(300);
    }

    @Test
    @DisplayName("Ćwiczenie 2: brak towaru → false i żadnej zmiany; wszystko jest → true i zapis")
    void exercise2() {
        Map<String, Integer> missingOil = new LinkedHashMap<>();
        missingOil.put("SPO-001", 2);
        missingOil.put("SPO-003", 1);
        assertThat(exercises.reserveAllOrNothing(missingOil)).isFalse();
        assertThat(quantity("SPO-001")).as("kawa nie zdjęta").isEqualTo(120);

        Map<String, Integer> available = new LinkedHashMap<>();
        available.put("SPO-001", 2);
        available.put("SPO-002", 5);
        assertThat(exercises.reserveAllOrNothing(available)).isTrue();
        assertThat(quantity("SPO-001")).isEqualTo(118);
        assertThat(quantity("SPO-002")).isEqualTo(295);
    }

    @Test
    @DisplayName("Ćwiczenie 3: każda dostawa w osobnej transakcji — nieudane wycofane, udane zapisane")
    void exercise3() {
        Map<String, Integer> deliveries = new LinkedHashMap<>();
        deliveries.put("SPO-001", 10);      // 120 → 130: OK
        deliveries.put("SPO-002", 800);     // 300 → 1100 > 1000: wycofać
        deliveries.put("SPO-999", 5);       // nie ma takiego sku: wycofać
        deliveries.put("SPO-003", 7);       // 0 → 7: OK

        assertThat(exercises.restockEachSeparately(deliveries)).containsExactly("SPO-002", "SPO-999");
        assertThat(quantity("SPO-001")).isEqualTo(130);
        assertThat(quantity("SPO-002")).as("dostawa ponad limit wycofana").isEqualTo(300);
        assertThat(quantity("SPO-003")).isEqualTo(7);
    }
}
