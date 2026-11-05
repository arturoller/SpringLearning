package s10_transactions.tx01_transactional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 5 lekcji Tx01Transactional — PUŁAPKA testów: {@code @DataJpaTest} (data JPA test = test warstwy JPA) jest
 * {@code @Transactional}. Cały test to JEDNA transakcja wycofywana na końcu, a metody serwisu z REQUIRED dołączają się
 * do niej — więc test NIE widzi ani commitu, ani rollbacku serwisu. Prawdziwe wyniki: Tx01TransactionalTest.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("tx01")
@Import({TransferService.class, FeeService.class}) // wycinek JPA nie skanuje @Service — dołączamy serwisy ręcznie
class Tx01DataJpaTestPitfallTest {

    @Autowired
    private TransferService transfers;
    @Autowired
    private FeeService fees;
    @Autowired
    private WalletRepository wallets;
    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("Sekcja 5: w transakcji testu „wycofany” przelew wygląda na niewycofany — encja w pamięci ma saldo 400")
    void transactionalTestHidesRollback() {
        assertThatThrownBy(() -> transfers.transfer(1L, 99L, new BigDecimal("100.00")))
                .isInstanceOf(WalletNotFoundException.class);

        // Serwis dołączył do transakcji TESTU. Rollback serwisu = tylko flaga „rollback-only” na transakcji testu;
        // encja w kontekście utrwalania (pamięć Hibernate) dalej ma zmienione saldo.
        assertThat(wallets.findById(1L).orElseThrow().getBalance()).isEqualByComparingTo("400.00");
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
    }

    @Test
    @DisplayName("Sekcja 5: w transakcji testu nie odróżnisz rollbackFor od braku rollbackFor — oba „zostawiają” opłatę")
    void transactionalTestCannotTellCommitFromRollback() {
        assertThatThrownBy(() -> fees.withdrawWithFee(2L, new BigDecimal("500.00")));
        assertThatThrownBy(() -> fees.withdrawWithFeeOrRollback(2L, new BigDecimal("500.00")));

        assertThat(wallets.findById(2L).orElseThrow().getBalance()).as("dwie opłaty w pamięci, nic nie wiadomo o bazie")
                .isEqualByComparingTo("96.00");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // bez transakcji testu — widać prawdę
    @DisplayName("Sekcja 5: z Propagation.NOT_SUPPORTED na teście ten sam przelew pokazuje prawdziwy rollback")
    void notSupportedShowsRealRollback() {
        assertThatThrownBy(() -> transfers.transfer(1L, 99L, new BigDecimal("100.00")))
                .isInstanceOf(WalletNotFoundException.class);

        assertThat(Tx01TestData.balance(jdbc, 1)).isEqualByComparingTo("500.00");
    }
}
