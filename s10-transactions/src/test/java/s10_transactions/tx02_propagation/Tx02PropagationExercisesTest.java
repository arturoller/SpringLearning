package s10_transactions.tx02_propagation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Tx02Propagation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("tx02")
class Tx02PropagationExercisesTest {

    @Autowired
    Tx02Exercises exercises;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanTables() {
        jdbc.sql("delete from orders").update();
        jdbc.sql("delete from audit_log").update();
    }

    int count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
    }

    @Test
    @DisplayName("Ćwiczenie 1: wpis audytu przetrwał wycofanie transakcji wołającego")
    void exercise1() {
        transactionTemplate.executeWithoutResult(status -> {
            exercises.logEvenIfCallerFails("Próba logowania do panelu");
            status.setRollbackOnly();                     // wołający się wycofuje
        });
        assertThat(count("audit_log")).isEqualTo(1);
    }

    @Test
    @DisplayName("Ćwiczenie 2: anulowanie zamówienia tylko wewnątrz transakcji")
    void exercise2() {
        jdbc.sql("insert into orders (product, quantity) values ('Kawa', 1)").update();
        long id = jdbc.sql("select id from orders where product = 'Kawa'").query(Long.class).single();

        assertThatThrownBy(() -> exercises.cancelOrderInsideTransaction(id))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(count("orders")).isEqualTo(1);

        transactionTemplate.executeWithoutResult(status -> exercises.cancelOrderInsideTransaction(id));
        assertThat(count("orders")).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 3: nieudany audyt nie psuje zamówienia")
    void exercise3() {
        exercises.placeOrderWithSafeAudit("Kawa", 2, null);
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("audit_log")).isZero();

        exercises.placeOrderWithSafeAudit("Herbata", 1, "Zamówienie: Herbata");
        assertThat(count("orders")).isEqualTo(2);
        assertThat(count("audit_log")).isEqualTo(1);
    }
}
