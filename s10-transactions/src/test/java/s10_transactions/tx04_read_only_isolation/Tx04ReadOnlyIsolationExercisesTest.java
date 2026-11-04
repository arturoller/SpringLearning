package s10_transactions.tx04_read_only_isolation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import s10_transactions.SqlCapture;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Tx04ReadOnlyIsolation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Pełna aplikacja BEZ transakcji testu; „drugi użytkownik” (OtherUser, REQUIRES_NEW) działa w {@code meanwhile}.
 */
@Tag("cwiczenie")
@SpringBootTest(classes = Tx04Application.class, properties = SqlCapture.PROPERTY)
@ActiveProfiles("tx04")
class Tx04ReadOnlyIsolationExercisesTest {

    @Autowired
    private ReadOnlyIsolationExercises exercises;
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
    @DisplayName("Ćwiczenie 1: podgląd zmiany w transakcji read-only — nowa nazwa w wyniku, w bazie bez zmian")
    void exercise1() {
        assertThat(exercises.previewRename(1, "Ewa Lis"))
                .isEqualTo(new ReadOnlyIsolationExercises.RenamePreview("Ewa Lis", true));

        assertThat(Tx04TestData.owner(jdbc, 1)).isEqualTo("Anna Nowak");
    }

    @Test
    @DisplayName("Ćwiczenie 2: wypłata z blokadą — SELECT … for update, saldo zapisane; za mało środków → wyjątek")
    void exercise2() {
        assertThat(exercises.withdrawWithLock(1, new BigDecimal("30.00"))).isEqualByComparingTo("70.00");

        assertThat(SqlCapture.startingWith("select")).singleElement().asString().endsWith(" for update");
        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("70.00");

        assertThatThrownBy(() -> exercises.withdrawWithLock(2, new BigDecimal("80.00")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(Tx04TestData.balance(jdbc, 2)).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Ćwiczenie 3: dwa odczyty dają 100.00 mimo cudzego commitu — poziom REPEATABLE READ")
    void exercise3() {
        ReadOnlyIsolationExercises.TwoReads reads =
                exercises.readBalanceTwiceConsistently(1, () -> otherUser.topUp(1, new BigDecimal("50.00")));

        assertThat(reads.first()).isEqualByComparingTo("100.00");
        assertThat(reads.second()).isEqualByComparingTo("100.00");
        assertThat(reads.isolationLevel()).isEqualTo("REPEATABLE READ");
        assertThat(Tx04TestData.balance(jdbc, 1)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Ćwiczenie 4: konflikt wersji → ponowienie w nowej transakcji; obie wpłaty zapisane (180.00), 2 próby")
    void exercise4() {
        int attempts = exercises.topUpWithRetry(1, new BigDecimal("30.00"),
                () -> otherUser.topUpVersioned(1, new BigDecimal("50.00")));

        assertThat(attempts).isEqualTo(2);
        assertThat(Tx04TestData.versionedBalance(jdbc, 1)).isEqualByComparingTo("180.00");
        assertThat(Tx04TestData.version(jdbc, 1)).isEqualTo(2);
    }
}
