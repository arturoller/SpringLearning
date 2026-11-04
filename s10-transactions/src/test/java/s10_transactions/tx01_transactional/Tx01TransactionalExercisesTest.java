package s10_transactions.tx01_transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Tx01Transactional (TransactionalExercises). @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Pełna aplikacja BEZ transakcji testu: saldo czytane JdbcClientem to prawdziwy stan bazy po commicie/rollbacku.
 */
@Tag("cwiczenie")
@SpringBootTest(classes = Tx01Application.class)
@ActiveProfiles("tx01")
class Tx01TransactionalExercisesTest {

    @Autowired
    private TransactionalExercises exercises;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx01TestData.reset(jdbc);
    }

    private BigDecimal balance(long walletId) {
        return Tx01TestData.balance(jdbc, walletId);
    }

    @Test
    @DisplayName("Ćwiczenie 1: całe saldo przeniesione; brak drugiego portfela → nic się nie zmienia")
    void exercise1() {
        exercises.transferAll(1L, 2L);
        assertThat(balance(1)).isEqualByComparingTo("0.00");
        assertThat(balance(2)).isEqualByComparingTo("600.00");

        assertThatThrownBy(() -> exercises.transferAll(2L, 99L)).isInstanceOf(WalletNotFoundException.class);
        assertThat(balance(2)).as("wypłata musi zostać wycofana").isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("Ćwiczenie 2: faktura opłacona z opłatą; brak środków → wyjątek checked i saldo bez zmian")
    void exercise2() throws Exception {
        exercises.payInvoice(1L, new BigDecimal("100.00"));
        assertThat(balance(1)).isEqualByComparingTo("398.00");

        assertThatThrownBy(() -> exercises.payInvoice(2L, new BigDecimal("1000.00")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(balance(2)).as("opłata też musi zostać wycofana").isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Ćwiczenie 3: wypłata ze wszystkich portfeli albo z żadnego — false bez wyjątku i bez zmian")
    void exercise3() {
        assertThat(exercises.withdrawFromAll(List.of(1L, 2L), new BigDecimal("50.00"))).isTrue();
        assertThat(balance(1)).isEqualByComparingTo("450.00");
        assertThat(balance(2)).isEqualByComparingTo("50.00");

        assertThat(exercises.withdrawFromAll(List.of(1L, 2L), new BigDecimal("200.00"))).isFalse();
        assertThat(balance(1)).as("wypłata z portfela 1 musi zostać wycofana").isEqualByComparingTo("450.00");
        assertThat(balance(2)).isEqualByComparingTo("50.00");
    }
}
