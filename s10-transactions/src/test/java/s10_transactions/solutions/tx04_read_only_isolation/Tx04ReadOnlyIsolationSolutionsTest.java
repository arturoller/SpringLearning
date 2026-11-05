package s10_transactions.solutions.tx04_read_only_isolation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s10_transactions.SqlCapture;
import s10_transactions.tx04_read_only_isolation.OtherUser;
import s10_transactions.tx04_read_only_isolation.ReadOnlyIsolationExercises.RenamePreview;
import s10_transactions.tx04_read_only_isolation.ReadOnlyIsolationExercises.TwoReads;
import s10_transactions.tx04_read_only_isolation.Tx04TestData;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprawdzenia z Tx04ReadOnlyIsolationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze: dowód,
 * że ćwiczenia da się rozwiązać, a ich testy są poprawne. Własna baza ({@code tx04-solutions}).
 */
@Tag("wzorzec")
@SpringBootTest(classes = Tx04SolutionsApplication.class,
        properties = {SqlCapture.PROPERTY, "spring.datasource.url=jdbc:h2:mem:tx04-solutions"})
@ActiveProfiles("tx04")
class Tx04ReadOnlyIsolationSolutionsTest {

    @Autowired
    private SolutionReadOnlyIsolationExercises exercises;
    @Autowired
    private OtherUser otherUser;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx04TestData.reset(jdbc);
        SqlCapture.clear();
    }

    @Test
    @DisplayName("Wzorzec 1: podgląd zmiany w transakcji read-only — nowa nazwa w wyniku, w bazie bez zmian")
    void exercise1() {
        assertThat(exercises.previewRename(1, "Ewa Lis"))
                .isEqualTo(new RenamePreview("Ewa Lis", true));

        assertThat(Tx04TestData.owner(jdbc, 1)).isEqualTo("Anna Nowak");
    }

    @Test
    @DisplayName("Wzorzec 2: wypłata z blokadą — SELECT … for update, saldo zapisane; za mało środków → wyjątek")
    void exercise2() {
        assertThat(exercises.withdrawWithLock(1, new BigDecimal("30.00"))).isEqualByComparingTo("70.00");

        assertThat(SqlCapture.startingWith("select")).singleElement().asString().endsWith(" for update");
        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("70.00");

        assertThatThrownBy(() -> exercises.withdrawWithLock(2, new BigDecimal("80.00")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(Tx04TestData.balance(jdbc, 2)).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Wzorzec 3: dwa odczyty dają 100.00 mimo cudzego commitu — poziom REPEATABLE READ")
    void exercise3() {
        TwoReads reads =
                exercises.readBalanceTwiceConsistently(1, () -> otherUser.topUp(1, new BigDecimal("50.00")));

        assertThat(reads.first()).isEqualByComparingTo("100.00");
        assertThat(reads.second()).isEqualByComparingTo("100.00");
        assertThat(reads.isolationLevel()).isEqualTo("REPEATABLE READ");
        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Wzorzec 4: konflikt wersji → ponowienie w nowej transakcji; obie wpłaty zapisane (180.00), 2 próby")
    void exercise4() {
        int attempts = exercises.topUpWithRetry(1, new BigDecimal("30.00"),
                () -> otherUser.topUpVersioned(1, new BigDecimal("50.00")));

        assertThat(attempts).isEqualTo(2);
        assertThat(Tx04TestData.versionedBalance(jdbc, 1)).isEqualByComparingTo("180.00");
        assertThat(Tx04TestData.version(jdbc, 1)).isEqualTo(2);
    }
}
