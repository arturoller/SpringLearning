package s10_transactions.tx02_propagation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx02Propagation — „WYNIK” każdej sekcji.
 *
 * <p>Jak w Tx01: {@code @SpringBootTest} BEZ transakcji testu (inaczej transakcja testu byłaby „zewnętrzną” dla
 * wszystkich wywołań i nie zobaczylibyśmy prawdziwych commitów). Stan bazy czytamy JdbcClientem, a przed każdym
 * testem czyścimy obie tabele.</p>
 */
@SpringBootTest
@ActiveProfiles("tx02")
class Tx02PropagationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    AuditService auditService;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanTables() {
        jdbc.sql("delete from orders").update();
        jdbc.sql("delete from audit_log").update();
    }

    int orders() {
        return jdbc.sql("select count(*) from orders").query(Integer.class).single();
    }

    int auditEntries() {
        return jdbc.sql("select count(*) from audit_log").query(Integer.class).single();
    }

    @Test
    @DisplayName("1. REQUIRED dołącza do transakcji wołającego: ta sama nazwa transakcji, wspólny rollback")
    void requiredJoins() {
        var names = orderService.transactionNames();
        assertThat(names.get(1)).as("REQUIRED — nazwa transakcji WOŁAJĄCEGO").isEqualTo(names.get(0))
                .endsWith("OrderService.transactionNames");
        assertThat(names.get(2)).as("REQUIRES_NEW — własna transakcja").endsWith("AuditService.transactionNameRequiresNew");

        assertThatThrownBy(() -> orderService.placeOrderThenFail("Kawa", 1, Propagation.REQUIRED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(orders()).isZero();
        assertThat(auditEntries()).as("wpis audytu wycofany razem z zamówieniem").isZero();
    }

    @Test
    @DisplayName("2. REQUIRES_NEW: wpis audytu zostaje, choć zamówienie wycofano; błąd audytu nie psuje zamówienia")
    void requiresNewIsIndependent() {
        assertThatThrownBy(() -> orderService.placeOrderThenFail("Kawa", 1, Propagation.REQUIRES_NEW))
                .isInstanceOf(IllegalStateException.class);
        assertThat(orders()).isZero();
        assertThat(auditEntries()).as("audyt zatwierdzony we własnej transakcji").isEqualTo(1);

        cleanTables();
        orderService.placeOrderIgnoringAuditFailure("Herbata", 2, Propagation.REQUIRES_NEW);
        assertThat(orders()).as("zamówienie zatwierdzone").isEqualTo(1);
        assertThat(auditEntries()).as("wewnętrzna transakcja audytu wycofana").isZero();
    }

    @Test
    @DisplayName("3. REQUIRED + złapany wyjątek → UnexpectedRollbackException, nic się nie zapisuje")
    void unexpectedRollback() {
        assertThatThrownBy(() -> orderService.placeOrderIgnoringAuditFailure("Kawa", 1, Propagation.REQUIRED))
                .isInstanceOf(UnexpectedRollbackException.class)
                .hasMessageContaining("rollback-only");
        assertThat(orders()).as("zamówienie też wycofane").isZero();
        assertThat(auditEntries()).isZero();

        // To samo, gdy wyjątek rzuci REPOZYTORIUM (null w kolumnie NOT NULL): save() też jest @Transactional (REQUIRED),
        // więc to jego proxy oznacza wspólną transakcję jako rollback-only — try/catch wokół logRequired nie pomoże.
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            try {
                auditService.logRequired(null);
            } catch (RuntimeException ignored) {
                // „połknięty” wyjątek
            }
        })).isInstanceOf(UnexpectedRollbackException.class);
    }

    @Test
    @DisplayName("4. MANDATORY i NEVER pilnują kontraktu, SUPPORTS i NOT_SUPPORTED dopasowują się do wołającego")
    void mandatoryNeverSupportsNotSupported() {
        assertThatThrownBy(() -> auditService.logMandatory("bez transakcji"))
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessageContaining("No existing transaction found");
        orderService.placeOrderWithMandatoryAudit("Kawa", 1);
        assertThat(auditEntries()).isEqualTo(1);

        assertThatThrownBy(() -> orderService.callNeverInsideTransaction())
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessageContaining("Existing transaction found");
        assertThat(auditService.neverSeesTransaction()).isFalse();

        assertThat(auditService.supportsSeesTransaction()).as("SUPPORTS bez transakcji").isFalse();
        assertThat(orderService.probesInsideTransaction()).as("[SUPPORTS, NOT_SUPPORTED] w transakcji")
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("5. NESTED z JpaTransactionManager: wewnątrz transakcji → NestedTransactionNotSupportedException")
    void nestedWithJpa() {
        assertThatThrownBy(() -> orderService.placeOrderThenFail("Kawa", 1, Propagation.NESTED))
                .isInstanceOf(NestedTransactionNotSupportedException.class)
                .hasMessageContaining("does not allow nested transactions by default");
        assertThat(orders()).isZero();

        auditService.logNested("bez transakcji wołającego");
        assertThat(auditEntries()).as("bez transakcji zewnętrznej NESTED działa jak REQUIRED").isEqualTo(1);
    }

    @Test
    @DisplayName("5. NESTED z menedżerem JDBC: punkt zapisu cofa tylko część wewnętrzną")
    void nestedWithJdbcSavepoint() {
        var jdbcTransactions = new JdbcTransactionManager(dataSource);
        var outer = new TransactionTemplate(jdbcTransactions);
        var nested = new TransactionTemplate(jdbcTransactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

        outer.executeWithoutResult(status -> {
            jdbc.sql("insert into orders (product, quantity) values ('Kawa', 1)").update();
            nested.executeWithoutResult(inner -> {
                jdbc.sql("insert into audit_log (message) values ('wpis do cofnięcia')").update();
                inner.setRollbackOnly();          // cofnij do punktu zapisu (savepoint)
            });
        });

        assertThat(orders()).as("część zewnętrzna zatwierdzona").isEqualTo(1);
        assertThat(auditEntries()).as("część zagnieżdżona cofnięta do punktu zapisu").isZero();
    }
}
