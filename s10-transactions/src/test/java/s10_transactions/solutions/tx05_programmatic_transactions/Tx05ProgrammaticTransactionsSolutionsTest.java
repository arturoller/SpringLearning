package s10_transactions.solutions.tx05_programmatic_transactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import s10_transactions.tx05_programmatic_transactions.FakeEmailSender;
import s10_transactions.tx05_programmatic_transactions.OrderLine;
import s10_transactions.tx05_programmatic_transactions.ProgrammaticExercises.ReportSnapshot;
import s10_transactions.tx05_programmatic_transactions.Tx05TestData;
import s10_transactions.tx05_programmatic_transactions.TxState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprawdzenia z Tx05ProgrammaticTransactionsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze:
 * dowód, że ćwiczenia da się rozwiązać, a ich testy są poprawne. Własna baza ({@code tx05-solutions}).
 */
@Tag("wzorzec")
@SpringBootTest(classes = Tx05SolutionsApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:tx05-solutions")
@ActiveProfiles("tx05")
class Tx05ProgrammaticTransactionsSolutionsTest {

    @Autowired
    private SolutionProgrammaticExercises exercises;
    @Autowired
    private FakeEmailSender mail;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx05TestData.reset(jdbc);
        mail.clear();
    }

    @Test
    @DisplayName("Wzorzec 1: próba na sucho — w środku widać 1 zamówienie, po wyjściu baza pusta")
    void exercise1() {
        assertThat(exercises.dryRunCount(OrderLine.of("ZAM-1", "10.00"))).isEqualTo(1);

        assertThat(Tx05TestData.orders(jdbc)).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: import z transakcją na pozycję — zwraca odrzucone, zapisuje poprawne")
    void exercise2() {
        List<String> failed = exercises.importReturningFailures(List.of(OrderLine.of("ZAM-1", "10.00"),
                OrderLine.of("ZAM-2", "0.00"), OrderLine.of("ZAM-3", "5.00"), OrderLine.of("ZAM-3", "6.00")));

        assertThat(failed).containsExactly("ZAM-2", "ZAM-3");
        assertThat(Tx05TestData.orders(jdbc)).containsExactly("ZAM-1", "ZAM-3");
    }

    @Test
    @DisplayName("Wzorzec 3: własny szablon read-only z REPEATABLE READ i nazwą; wspólny bean bez zmian")
    void exercise3() {
        ReportSnapshot snapshot = exercises.readOnlyReport();

        assertThat(snapshot.state()).isEqualTo(new TxState(true, "exerciseReport", true));
        assertThat(snapshot.isolationLevel()).isEqualTo("REPEATABLE READ");
        assertThat(tx.isReadOnly()).isFalse();
        assertThat(tx.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
        assertThat(tx.getName()).isNull();
    }

    @Test
    @DisplayName("Wzorzec 4: e-maile dopiero po commicie; błąd jednej pozycji — nic nie zapisane i żaden e-mail")
    void exercise4() {
        exercises.placeOrdersAndNotify(List.of(OrderLine.of("ZAM-1", "10.00"), OrderLine.of("ZAM-2", "20.00")));
        assertThat(mail.sent()).containsExactly("klient@example.com: Przyjęliśmy zamówienie ZAM-1",
                "klient@example.com: Przyjęliśmy zamówienie ZAM-2");

        mail.clear();
        assertThatThrownBy(() -> exercises.placeOrdersAndNotify(List.of(OrderLine.of("ZAM-3", "10.00"),
                OrderLine.of("ZAM-4", "0.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(mail.sent()).isEmpty();
        assertThat(Tx05TestData.orders(jdbc)).containsExactly("ZAM-1", "ZAM-2");
    }
}
