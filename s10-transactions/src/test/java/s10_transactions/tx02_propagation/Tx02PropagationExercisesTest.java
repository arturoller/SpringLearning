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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Tx02Propagation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Pełna aplikacja BEZ transakcji testu; transakcję „wywołującego” otwiera tu TransactionTemplate
 * ({@code setRollbackOnly()} = wycofaj ją na końcu).
 */
@Tag("cwiczenie")
@SpringBootTest(classes = Tx02Application.class)
@ActiveProfiles("tx02")
class Tx02PropagationExercisesTest {

    @Autowired
    private PropagationExercises exercises;
    @Autowired
    private ExerciseAuditLog audit;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx02TestData.reset(jdbc);
    }

    @Test
    @DisplayName("Ćwiczenie 1: wpis audytu zostaje, choć transakcja wywołującego została wycofana")
    void exercise1() {
        tx.executeWithoutResult(status -> {
            audit.record("Próba zamówienia ZAM-1");
            status.setRollbackOnly();
        });

        assertThat(Tx02TestData.auditMessages(jdbc)).containsExactly("Próba zamówienia ZAM-1");
    }

    @Test
    @DisplayName("Ćwiczenie 2: brak towaru → zamówienie WAITING zapisane, bez UnexpectedRollbackException")
    void exercise2() {
        assertThat(exercises.placeOrderOrWait("ZAM-1", "Mysz", 3)).isEqualTo("WAITING");
        assertThat(exercises.placeOrderOrWait("ZAM-2", "Mysz", 2)).isEqualTo("RESERVED");

        assertThat(Tx02TestData.orders(jdbc)).containsExactly("ZAM-1:WAITING", "ZAM-2:RESERVED");
        assertThat(Tx02TestData.stock(jdbc, "Mysz")).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 3: wpis do księgi tylko w trwającej transakcji — dzieli jej los; bez transakcji wyjątek")
    void exercise3() {
        tx.executeWithoutResult(status -> audit.postToLedger("A"));
        tx.executeWithoutResult(status -> {
            audit.postToLedger("B");
            status.setRollbackOnly();
        });
        assertThat(Tx02TestData.auditMessages(jdbc)).containsExactly("KSIĘGA: A");

        assertThatThrownBy(() -> audit.postToLedger("C")).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(Tx02TestData.auditMessages(jdbc)).containsExactly("KSIĘGA: A");
    }

    @Test
    @DisplayName("Ćwiczenie 4: import — duplikaty pominięte, a przerwany import zostawia wcześniej zaimportowane")
    void exercise4() {
        assertThat(exercises.importOrders(List.of("ZAM-A", "ZAM-B", "ZAM-A", "ZAM-C"))).isEqualTo(3);

        assertThatThrownBy(() -> exercises.importOrders(List.of("ZAM-D", " ", "ZAM-E")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Tx02TestData.orders(jdbc))
                .containsExactly("ZAM-A:IMPORTED", "ZAM-B:IMPORTED", "ZAM-C:IMPORTED", "ZAM-D:IMPORTED");
    }
}
