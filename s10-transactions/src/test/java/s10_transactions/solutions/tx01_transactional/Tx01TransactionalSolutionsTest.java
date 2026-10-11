package s10_transactions.solutions.tx01_transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s10_transactions.tx01_transactional.AccountNotFoundException;
import s10_transactions.tx01_transactional.PaymentGatewayException;
import s10_transactions.tx01_transactional.ReceiptPrinterException;
import s10_transactions.tx01_transactional.Tx01Application;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprawdzenia z Tx01TransactionalExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Klasa rozwiązań leży poza skanowanym pakietem lekcji, więc rejestrujemy ją wprost w {@code classes}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = {Tx01Application.class, Tx01TransactionalSolutions.class})
@ActiveProfiles("tx01")
class Tx01TransactionalSolutionsTest {

    @Autowired
    Tx01TransactionalSolutions solutions;

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
    @DisplayName("Wzorzec 1: przelew ponad limit → wyjątek kontrolowany i rollback; w limicie → commit")
    void solution1() throws Exception {
        assertThatThrownBy(() -> solutions.transferWithLimit("PL-ANNA", "PL-BARTEK", 60000))
                .isInstanceOf(PaymentGatewayException.class);
        assertThat(balance("PL-ANNA")).isEqualTo(100000);
        assertThat(balance("PL-BARTEK")).isEqualTo(50000);

        solutions.transferWithLimit("PL-ANNA", "PL-BARTEK", 20000);
        assertThat(balance("PL-ANNA")).isEqualTo(80000);
        assertThat(balance("PL-BARTEK")).isEqualTo(70000);
    }

    @Test
    @DisplayName("Wzorzec 2: drukarka się psuje, a obciążenie konta zostaje")
    void solution2() {
        assertThatThrownBy(() -> solutions.chargeWithBrokenPrinter("PL-BARTEK", 1500))
                .isInstanceOf(ReceiptPrinterException.class);
        assertThat(balance("PL-BARTEK")).isEqualTo(48500);
    }

    @Test
    @DisplayName("Wzorzec 3: płatności wszystkie albo żadna")
    void solution3() {
        Map<String, Long> ok = new LinkedHashMap<>();
        ok.put("PL-BARTEK", 10000L);
        ok.put("PL-CEZARY", 5000L);
        assertThat(solutions.payAll("PL-ANNA", ok)).isEqualTo(15000);
        assertThat(balance("PL-ANNA")).isEqualTo(85000);
        assertThat(balance("PL-CEZARY")).isEqualTo(5000);

        Map<String, Long> broken = new LinkedHashMap<>();
        broken.put("PL-BARTEK", 10000L);
        broken.put("PL-NIEMA", 5000L);
        assertThatThrownBy(() -> solutions.payAll("PL-ANNA", broken)).isInstanceOf(AccountNotFoundException.class);
        assertThat(balance("PL-ANNA")).as("nic nie zostało zapłacone").isEqualTo(85000);
        assertThat(balance("PL-BARTEK")).isEqualTo(60000);

        Map<String, Long> tooMuch = new LinkedHashMap<>();
        tooMuch.put("PL-CEZARY", 80000L);
        tooMuch.put("PL-BARTEK", 10000L);
        assertThatThrownBy(() -> solutions.payAll("PL-ANNA", tooMuch)).isInstanceOf(IllegalStateException.class);
        assertThat(balance("PL-ANNA")).isEqualTo(85000);
        assertThat(balance("PL-CEZARY")).isEqualTo(5000);
    }
}
