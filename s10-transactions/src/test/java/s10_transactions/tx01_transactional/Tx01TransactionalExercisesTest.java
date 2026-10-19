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
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-JAN', 'Jan Kowalski', 100000)").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-MARIA', 'Maria Nowak', 50000)").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-ADAM', 'Adam Mazur', 0)").update();
    }

    long balance(String number) {
        return jdbc.sql("select balance from accounts where number = ?").param(number).query(Long.class).single();
    }

    @Test
    @DisplayName("Ćwiczenie 1: przelew ponad limit → wyjątek kontrolowany i rollback; w limicie → commit")
    void exercise1() throws Exception {
        assertThatThrownBy(() -> exercises.transferWithLimit("PL-JAN", "PL-MARIA", 60000))
                .isInstanceOf(PaymentGatewayException.class);
        assertThat(balance("PL-JAN")).isEqualTo(100000);
        assertThat(balance("PL-MARIA")).isEqualTo(50000);

        exercises.transferWithLimit("PL-JAN", "PL-MARIA", 20000);
        assertThat(balance("PL-JAN")).isEqualTo(80000);
        assertThat(balance("PL-MARIA")).isEqualTo(70000);
    }

    @Test
    @DisplayName("Ćwiczenie 2: drukarka się psuje, a obciążenie konta zostaje")
    void exercise2() {
        assertThatThrownBy(() -> exercises.chargeWithBrokenPrinter("PL-MARIA", 1500))
                .isInstanceOf(ReceiptPrinterException.class);
        assertThat(balance("PL-MARIA")).isEqualTo(48500);
    }

    @Test
    @DisplayName("Ćwiczenie 3: płatności wszystkie albo żadna")
    void exercise3() {
        Map<String, Long> ok = new LinkedHashMap<>();
        ok.put("PL-MARIA", 10000L);
        ok.put("PL-ADAM", 5000L);
        assertThat(exercises.payAll("PL-JAN", ok)).isEqualTo(15000);
        assertThat(balance("PL-JAN")).isEqualTo(85000);
        assertThat(balance("PL-ADAM")).isEqualTo(5000);

        Map<String, Long> broken = new LinkedHashMap<>();
        broken.put("PL-MARIA", 10000L);
        broken.put("PL-NIEMA", 5000L);
        assertThatThrownBy(() -> exercises.payAll("PL-JAN", broken)).isInstanceOf(AccountNotFoundException.class);
        assertThat(balance("PL-JAN")).as("nic nie zostało zapłacone").isEqualTo(85000);
        assertThat(balance("PL-MARIA")).isEqualTo(60000);

        Map<String, Long> tooMuch = new LinkedHashMap<>();
        tooMuch.put("PL-ADAM", 80000L);
        tooMuch.put("PL-MARIA", 10000L);
        assertThatThrownBy(() -> exercises.payAll("PL-JAN", tooMuch)).isInstanceOf(IllegalStateException.class);
        assertThat(balance("PL-JAN")).isEqualTo(85000);
        assertThat(balance("PL-ADAM")).isEqualTo(5000);
    }
}
