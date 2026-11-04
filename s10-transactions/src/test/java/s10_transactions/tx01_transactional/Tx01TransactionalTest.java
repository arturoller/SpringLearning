package s10_transactions.tx01_transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx01Transactional na PEŁNEJ aplikacji (Tx01Application) — BEZ transakcji testu (brak
 * {@code @Transactional} na klasie testu). Dlaczego nie {@code @DataJpaTest}? Bo wycinek działa w transakcji
 * wycofywanej po teście i zakrywa commit/rollback (sekcja 5 lekcji, Tx01DataJpaTestPitfallTest).
 * Stan bazy przywraca {@code @BeforeEach}; saldo czytamy JdbcClientem — widzi tylko dane zatwierdzone.
 */
@SpringBootTest(classes = Tx01Application.class)
@ActiveProfiles("tx01")
@ExtendWith(OutputCaptureExtension.class) // przechwytywanie konsoli — do sprawdzenia logów menedżera transakcji
class Tx01TransactionalTest {

    private static final String TX_LOGGER = "org.springframework.orm.jpa.JpaTransactionManager";

    @Autowired
    private TransferService transfers;
    @Autowired
    private TransactionInspector inspector;
    @Autowired
    private FeeService fees;
    @Autowired
    private WalletSettingsService settings;
    @Autowired
    private JakartaWalletService jakarta;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        Tx01TestData.reset(jdbc);
    }

    @AfterEach
    void resetLogLevel() {
        LoggingSystem.get(getClass().getClassLoader()).setLogLevel(TX_LOGGER, null);
    }

    private BigDecimal balance(long walletId) {
        return Tx01TestData.balance(jdbc, walletId);
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: przelew BEZ transakcji i awaria w połowie — pieniądze znikają z pierwszego portfela")
    void section1WithoutTransactionMoneyDisappears() {
        assertThatThrownBy(() -> transfers.transferWithoutTransaction(1L, 99L, new BigDecimal("100.00")))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("Nie ma portfela o id 99");

        assertThat(balance(1)).as("wypłata zatwierdzona przez save() repozytorium").isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Sekcja 1: przelew z @Transactional — awaria w połowie wycofuje oba kroki, udany przelew zapisuje oba")
    void section1WithTransactionAllOrNothing() {
        assertThatThrownBy(() -> transfers.transfer(1L, 99L, new BigDecimal("100.00")))
                .isInstanceOf(WalletNotFoundException.class);
        assertThat(balance(1)).as("rollback wypłaty").isEqualByComparingTo("500.00");

        transfers.transfer(1L, 2L, new BigDecimal("100.00"));
        assertThat(balance(1)).isEqualByComparingTo("400.00");
        assertThat(balance(2)).isEqualByComparingTo("200.00");
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: bean serwisu to proxy CGLIB, menedżer to JpaTransactionManager, transakcja aktywna tylko w metodzie")
    void section2ProxyAndTransactionManager() {
        assertThat(AopUtils.isAopProxy(transfers)).isTrue();
        assertThat(AopUtils.isCglibProxy(transfers)).isTrue();
        assertThat(transfers.getClass().getName())
                .isEqualTo("s10_transactions.tx01_transactional.TransferService$$SpringCGLIB$$0");
        assertThat(transactionManager).isInstanceOf(JpaTransactionManager.class);

        assertThat(inspector.insideTransaction()).isEqualTo(new TransactionInfo(true,
                "s10_transactions.tx01_transactional.TransactionInspector.insideTransaction", false));
        assertThat(inspector.outsideTransaction()).isEqualTo(new TransactionInfo(false, null, false));
        assertThat(TransactionInfo.now().active()).as("w samym teście transakcji nie ma").isFalse();
    }

    @Test
    @DisplayName("Sekcja 2: w jednej transakcji JPA i JdbcClient używają tego samego połączenia (ta sama sesja H2)")
    void section2OneConnectionPerTransaction() {
        var ids = inspector.sessionIdsSeenByJpaAndJdbc();

        assertThat(ids.get(0)).isEqualTo(ids.get(1));
    }

    @Test
    @DisplayName("Sekcja 2: log JpaTransactionManager (DEBUG) pokazuje początek transakcji, commit i rollback")
    void section2TransactionManagerLog(CapturedOutput output) {
        LoggingSystem.get(getClass().getClassLoader()).setLogLevel(TX_LOGGER, LogLevel.DEBUG);

        transfers.transfer(1L, 2L, BigDecimal.ONE);
        assertThatThrownBy(() -> transfers.transfer(1L, 99L, BigDecimal.ONE));

        assertThat(output.getOut())
                .contains("Creating new transaction with name "
                        + "[s10_transactions.tx01_transactional.TransferService.transfer]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT")
                .contains("Initiating transaction commit")
                .contains("Initiating transaction rollback");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: wyjątek SPRAWDZANY domyślnie → COMMIT (opłata zostaje); rollbackFor → rollback")
    void section3CheckedExceptionCommitsByDefault() {
        assertThatThrownBy(() -> fees.withdrawWithFee(2L, new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessage("Portfel 2: żądano 500.00, saldo 98.00");
        assertThat(balance(2)).as("opłata ZATWIERDZONA mimo wyjątku").isEqualByComparingTo("98.00");

        assertThatThrownBy(() -> fees.withdrawWithFeeOrRollback(2L, new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(balance(2)).as("rollbackFor wycofał opłatę").isEqualByComparingTo("98.00");
    }

    @Test
    @DisplayName("Sekcja 3: noRollbackFor — wyjątek niesprawdzany, a mimo to COMMIT; Error — rollback")
    void section3NoRollbackForAndError() {
        assertThatThrownBy(() -> fees.withdrawOrKeepFee(2L, new BigDecimal("500.00")))
                .isInstanceOf(WithdrawalRejectedException.class);
        assertThat(balance(2)).as("noRollbackFor: opłata za próbę zostaje").isEqualByComparingTo("98.00");

        assertThatThrownBy(() -> fees.withdrawWithFeeAndCrash(2L)).isInstanceOf(LedgerCorruptedError.class);
        assertThat(balance(2)).as("Error wycofał drugą opłatę").isEqualByComparingTo("98.00");
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: wyjątek złapany w metodzie → COMMIT połowy przelewu; setRollbackOnly() → cichy rollback")
    void section4CaughtExceptionMeansCommit() {
        assertThat(transfers.transferSwallowingError(1L, 99L, new BigDecimal("100.00"))).isFalse();
        assertThat(balance(1)).as("proxy nie widziało wyjątku → commit wypłaty").isEqualByComparingTo("400.00");

        assertThat(transfers.transferOrRollbackQuietly(1L, 99L, new BigDecimal("100.00"))).isFalse();
        assertThat(balance(1)).as("setRollbackOnly → rollback bez wyjątku").isEqualByComparingTo("400.00");
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: @Transactional(readOnly = true) na klasie, a adnotacja na metodzie zastępuje ją w całości")
    void section5ClassLevelVersusMethodLevel() {
        assertThat(settings.inspectRead()).isEqualTo(new TransactionInfo(true,
                "s10_transactions.tx01_transactional.WalletSettingsService.inspectRead", true));

        TransactionInfo write = settings.rename(2L, "Jan Kowalski-Nowak");
        assertThat(write.readOnly()).isFalse();
        assertThat(Tx01TestData.owner(jdbc, 2)).isEqualTo("Jan Kowalski-Nowak");
    }

    @Test
    @DisplayName("Sekcja 5: jakarta.transaction.Transactional też działa — checked → commit, rollbackOn → rollback")
    void section5JakartaTransactional() {
        assertThat(jakarta.inspect()).isEqualTo(new TransactionInfo(true,
                "s10_transactions.tx01_transactional.JakartaWalletService.inspect", false));

        assertThatThrownBy(() -> jakarta.withdrawWithFee(2L, new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(balance(2)).isEqualByComparingTo("98.00");

        assertThatThrownBy(() -> jakarta.withdrawWithFeeOrRollback(2L, new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(balance(2)).as("rollbackOn wycofał opłatę").isEqualByComparingTo("98.00");
    }
}
