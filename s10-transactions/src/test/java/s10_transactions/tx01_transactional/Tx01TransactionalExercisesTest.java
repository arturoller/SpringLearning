package s10_transactions.tx01_transactional;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Tx01Transactional. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("tx01")
class Tx01TransactionalExercisesTest {

    @Autowired
    Tx01Exercises exercises;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void resetAccounts() {
        jdbc.sql("delete from accounts").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-ANNA', 'Anna', 100000)").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-BARTEK', 'Bartek', 50000)").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-CEZARY', 'Cezary', 0)").update();
    }

    long balance(String number) {
        return jdbc.sql("select balance from accounts where number = ?").param(number).query(Long.class).single();
    }

    @Test
    @DisplayName("Ćwiczenie 1: przelew ponad limit → wyjątek kontrolowany i rollback; w limicie → commit")
    void exercise1() throws Exception {
        assertThatThrownBy(() -> exercises.transferWithLimit("PL-ANNA", "PL-BARTEK", 60000))
                .isInstanceOf(PaymentGatewayException.class);
        assertThat(balance("PL-ANNA")).isEqualTo(100000);
        assertThat(balance("PL-BARTEK")).isEqualTo(50000);

        exercises.transferWithLimit("PL-ANNA", "PL-BARTEK", 20000);
        assertThat(balance("PL-ANNA")).isEqualTo(80000);
        assertThat(balance("PL-BARTEK")).isEqualTo(70000);
    }

    @Test
    @DisplayName("Ćwiczenie 2: drukarka się psuje, a obciążenie konta zostaje")
    void exercise2() {
        assertThatThrownBy(() -> exercises.chargeWithBrokenPrinter("PL-BARTEK", 1500))
                .isInstanceOf(ReceiptPrinterException.class);
        assertThat(balance("PL-BARTEK")).isEqualTo(48500);
    }

    @Test
    @DisplayName("Ćwiczenie 3: płatności wszystkie albo żadna")
    void exercise3() {
        Map<String, Long> ok = new LinkedHashMap<>();
        ok.put("PL-BARTEK", 10000L);
        ok.put("PL-CEZARY", 5000L);
        assertThat(exercises.payAll("PL-ANNA", ok)).isEqualTo(15000);
        assertThat(balance("PL-ANNA")).isEqualTo(85000);
        assertThat(balance("PL-CEZARY")).isEqualTo(5000);

        Map<String, Long> broken = new LinkedHashMap<>();
        broken.put("PL-BARTEK", 10000L);
        broken.put("PL-NIEMA", 5000L);
        assertThatThrownBy(() -> exercises.payAll("PL-ANNA", broken)).isInstanceOf(AccountNotFoundException.class);
        assertThat(balance("PL-ANNA")).as("nic nie zostało zapłacone").isEqualTo(85000);
        assertThat(balance("PL-BARTEK")).isEqualTo(60000);

        Map<String, Long> tooMuch = new LinkedHashMap<>();
        tooMuch.put("PL-CEZARY", 80000L);
        tooMuch.put("PL-BARTEK", 10000L);
        assertThatThrownBy(() -> exercises.payAll("PL-ANNA", tooMuch)).isInstanceOf(IllegalStateException.class);
        assertThat(balance("PL-ANNA")).isEqualTo(85000);
        assertThat(balance("PL-CEZARY")).isEqualTo(5000);
    }
}
