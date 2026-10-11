package s10_transactions.tx01_transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx01Transactional — „WYNIK” każdej sekcji.
 *
 * <p>Dlaczego {@code @SpringBootTest}, a nie {@code @DataJpaTest}? {@code @DataJpaTest} owija KAŻDY test w swoją
 * transakcję i na końcu ją wycofuje. Wtedy {@code @Transactional} serwisu tylko DOŁĄCZA do transakcji testu — nic nie
 * jest naprawdę zatwierdzane ani wycofywane w chwili wyjścia z metody serwisu, więc nie zobaczylibyśmy różnicy między
 * commitem a rollbackiem. Tutaj test NIE ma transakcji: każda metoda serwisu ma własną, prawdziwą transakcję, a stan
 * bazy sprawdzamy „z zewnątrz” przez JdbcClient (czysty SQL).</p>
 *
 * <p>Ponieważ dane są naprawdę zatwierdzane, przed KAŻDYM testem przywracamy stan startowy ({@code @BeforeEach}) —
 * testy nie zależą od kolejności uruchamiania.</p>
 */
@SpringBootTest
@ActiveProfiles("tx01")
class Tx01TransactionalTest {

    @Autowired
    TransferService service;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void resetAccounts() {
        jdbc.sql("delete from accounts").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-ANNA', 'Anna', 100000)").update();
        jdbc.sql("insert into accounts (number, owner, balance) values ('PL-BARTEK', 'Bartek', 50000)").update();
    }

    long balance(String number) {
        return jdbc.sql("select balance from accounts where number = ?").param(number).query(Long.class).single();
    }

    @Test
    @DisplayName("1. PRZED: bez @Transactional obciążenie zostaje w bazie, choć przelew się nie udał")
    void withoutTransactionMoneyDisappears() {
        assertThatThrownBy(() -> service.transferWithoutTransaction("PL-ANNA", "PL-NIEMA", 20000))
                .isInstanceOf(AccountNotFoundException.class);

        assertThat(balance("PL-ANNA")).as("200 zł zniknęło z konta Anny").isEqualTo(80000);
    }

    @Test
    @DisplayName("2. @Transactional: sukces → commit; RuntimeException → rollback (wszystko albo nic)")
    void transactionalCommitOrRollback() {
        service.transfer("PL-ANNA", "PL-BARTEK", 10000);
        assertThat(balance("PL-ANNA")).isEqualTo(90000);
        assertThat(balance("PL-BARTEK")).isEqualTo(60000);

        assertThatThrownBy(() -> service.transfer("PL-ANNA", "PL-NIEMA", 20000))
                .isInstanceOf(AccountNotFoundException.class);
        assertThat(balance("PL-ANNA")).as("obciążenie wycofane").isEqualTo(90000);

        assertThatThrownBy(() -> service.transfer("PL-BARTEK", "PL-ANNA", 999999))
                .isInstanceOf(IllegalStateException.class);
        assertThat(balance("PL-BARTEK")).isEqualTo(60000);
    }

    @Test
    @DisplayName("2. Error (np. OutOfMemoryError) też wycofuje transakcję")
    void errorRollsBack() {
        assertThatThrownBy(() -> service.transferThenCrash("PL-ANNA", "PL-BARTEK", 10000))
                .isInstanceOf(OutOfMemoryError.class);

        assertThat(balance("PL-ANNA")).isEqualTo(100000);
        assertThat(balance("PL-BARTEK")).isEqualTo(50000);
    }

    @Test
    @DisplayName("3. Wyjątek kontrolowany → domyślnie COMMIT; z rollbackFor → rollback")
    void checkedExceptionCommitsByDefault() {
        assertThatThrownBy(() -> service.transferAndConfirm("PL-ANNA", "PL-BARTEK", 10000, false))
                .isInstanceOf(PaymentGatewayException.class);
        assertThat(balance("PL-ANNA")).as("mimo wyjątku przelew ZATWIERDZONY").isEqualTo(90000);
        assertThat(balance("PL-BARTEK")).isEqualTo(60000);

        assertThatThrownBy(() -> service.transferAndConfirmOrRollback("PL-ANNA", "PL-BARTEK", 10000, false))
                .isInstanceOf(PaymentGatewayException.class);
        assertThat(balance("PL-ANNA")).as("rollbackFor → wycofany").isEqualTo(90000);
        assertThat(balance("PL-BARTEK")).isEqualTo(60000);
    }

    @Test
    @DisplayName("4. noRollbackFor: wyjątek leci dalej, ale obciążenie zostaje zatwierdzone")
    void noRollbackFor() {
        assertThatThrownBy(() -> service.chargeAndPrintReceipt("PL-ANNA", 2500, false))
                .isInstanceOf(ReceiptPrinterException.class);

        assertThat(balance("PL-ANNA")).isEqualTo(97500);
    }

    @Test
    @DisplayName("5. TransactionSynchronizationManager: transakcja jest tylko w metodzie z @Transactional")
    void observeTransaction() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).as("w samym teście").isFalse();
        assertThat(service.isTransactionActiveInside()).isTrue();
        assertThat(service.isTransactionActiveWithoutAnnotation()).isFalse();
        assertThat(service.isTransactionActiveWithJakartaAnnotation()).as("jakarta.transaction.Transactional").isTrue();
    }

    @Test
    @DisplayName("6. Pod spodem: bean jest proxy CGLIB, a transakcjami zarządza JpaTransactionManager")
    void proxyAndTransactionManager() {
        assertThat(AopUtils.isCglibProxy(service)).isTrue();
        assertThat(service.getClass().getName()).contains("$$SpringCGLIB$$");
        assertThat(AopUtils.getTargetClass(service)).isEqualTo(TransferService.class);
        assertThat(transactionManager).isInstanceOf(JpaTransactionManager.class);
        assertThat(context.getBeanNamesForType(PlatformTransactionManager.class)).containsExactly("transactionManager");
    }
}
