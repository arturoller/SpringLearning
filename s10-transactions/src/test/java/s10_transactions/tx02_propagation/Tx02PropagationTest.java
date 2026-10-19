package s10_transactions.tx02_propagation;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx02Propagation — „WYNIK” każdej sekcji.
 *
 * <p>Jak w Tx01: {@code @SpringBootTest} BEZ transakcji testu (inaczej transakcja testu byłaby „zewnętrzną” dla
 * wszystkich wywołań i nie zobaczylibyśmy prawdziwych commitów). Stan bazy czytamy JdbcClientem, a przed każdym
 * testem czyścimy obie tabele.</p>
 *
 * <p>Kilka testów buduje transakcje w kodzie przez {@code TransactionTemplate} (szablon transakcji — szczegóły
 * w Tx05ProgrammaticTransactions): {@code execute(...)} otwiera transakcję z podaną propagacją, wykonuje lambdę
 * i zatwierdza albo wycofuje. Dzięki temu możemy użyć menedżera transakcji z innymi ustawieniami niż bean
 * aplikacji.</p>
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
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PlatformTransactionManager transactionManager;

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

    TransactionTemplate requiresNew() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    @Test
    @DisplayName("1. REQUIRED dołącza do transakcji wołającego: ta sama nazwa transakcji, wspólny rollback")
    void requiredJoins() {
        var names = orderService.transactionNames();
        assertThat(names.get(1)).as("REQUIRED — nazwa transakcji WOŁAJĄCEGO").isEqualTo(names.get(0))
                .endsWith("OrderService.transactionNames");
        assertThat(names.get(2)).as("REQUIRES_NEW — własna transakcja").endsWith("AuditService.transactionNameRequiresNew");

        assertThatThrownBy(() -> orderService.placeOrderThenFail("Kawa ziarnista 1kg", 1, Propagation.REQUIRED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(orders()).isZero();
        assertThat(auditEntries()).as("wpis audytu wycofany razem z zamówieniem").isZero();
    }

    @Test
    @DisplayName("2. REQUIRES_NEW: wpis audytu zostaje, choć zamówienie wycofano; błąd audytu nie psuje zamówienia")
    void requiresNewIsIndependent() {
        assertThatThrownBy(() -> orderService.placeOrderThenFail("Kawa ziarnista 1kg", 1, Propagation.REQUIRES_NEW))
                .isInstanceOf(IllegalStateException.class);
        assertThat(orders()).isZero();
        assertThat(auditEntries()).as("audyt zatwierdzony we własnej transakcji").isEqualTo(1);

        cleanTables();
        orderService.placeOrderIgnoringAuditFailure("Czekolada gorzka", 2, Propagation.REQUIRES_NEW);
        assertThat(orders()).as("zamówienie zatwierdzone").isEqualTo(1);
        assertThat(auditEntries()).as("wewnętrzna transakcja audytu wycofana").isZero();
    }

    @Test
    @DisplayName("2. PUŁAPKA: REQUIRES_NEW trzyma DWA połączenia z puli naraz, REQUIRED — jedno")
    void requiresNewHoldsTwoConnections() {
        HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();   // podgląd puli HikariCP

        List<Integer> active = transactionTemplate.execute(outer -> List.of(
                pool.getActiveConnections(),                                              // T1
                transactionTemplate.execute(joined -> pool.getActiveConnections()),       // REQUIRED → nadal T1
                requiresNew().execute(inner -> pool.getActiveConnections())));            // REQUIRES_NEW → T1 + T2

        assertThat(active).containsExactly(1, 1, 2);

        // Domyślne ustawienia puli w Boot: 10 połączeń, na wolne połączenie czeka się do 30 s (30000 ms).
        assertThat(((HikariDataSource) dataSource).getMaximumPoolSize()).isEqualTo(10);
        assertThat(((HikariDataSource) dataSource).getConnectionTimeout()).isEqualTo(30_000);
    }

    @Test
    @DisplayName("2. PUŁAPKA: REQUIRES_NEW zmienia wiersz zablokowany przez wstrzymaną T1 → czeka na blokadę i pada")
    void requiresNewWaitsForLockOfSuspendedTransaction() {
        jdbc.sql("insert into orders (product, quantity) values ('Kawa ziarnista 1kg', 1)").update();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(outer -> {
            jdbc.sql("update orders set quantity = 2").update();                  // T1 zmienia (i blokuje) wiersz
            requiresNew().executeWithoutResult(inner ->
                    jdbc.sql("update orders set quantity = 3").update());         // T2 czeka na T1, a T1 — na T2
        })).isInstanceOf(QueryTimeoutException.class);                             // H2: limit czasu blokady (2 s)

        assertThat(jdbc.sql("select quantity from orders").query(Integer.class).single())
                .as("obie transakcje wycofane").isEqualTo(1);
    }

    @Test
    @DisplayName("3. REQUIRED + złapany wyjątek → UnexpectedRollbackException, nic się nie zapisuje")
    void unexpectedRollback() {
        assertThatThrownBy(() ->
                orderService.placeOrderIgnoringAuditFailure("Kawa ziarnista 1kg", 1, Propagation.REQUIRED))
                .isInstanceOf(UnexpectedRollbackException.class)
                .hasMessage("Transaction silently rolled back because it has been marked as rollback-only");
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
    @DisplayName("3. globalRollbackOnParticipationFailure = false: bez wyjątku, ale nieudany wpis audytu ZATWIERDZONY")
    void lenientParticipationFailureCommitsInconsistentData() {
        JpaTransactionManager lenient = new JpaTransactionManager(entityManagerFactory);
        lenient.setGlobalRollbackOnParticipationFailure(false);
        TransactionTemplate outer = new TransactionTemplate(lenient);
        TransactionTemplate participant = new TransactionTemplate(lenient);          // REQUIRED → uczestniczy w outer

        outer.executeWithoutResult(status -> {
            jdbc.sql("insert into orders (product, quantity) values ('Kawa ziarnista 1kg', 1)").update();
            try {
                participant.executeWithoutResult(inner -> {
                    jdbc.sql("insert into audit_log (message) values ('Zamówienie: Kawa ziarnista 1kg')").update();
                    throw new IllegalStateException("Audyt nie przyjął wpisu");
                });
            } catch (IllegalStateException ignored) {
                // „połykamy”
            }
        });

        assertThat(orders()).isEqualTo(1);
        assertThat(auditEntries()).as("wpis z „nieudanego” audytu też zatwierdzony").isEqualTo(1);
    }

    @Test
    @DisplayName("4. MANDATORY i NEVER pilnują kontraktu, SUPPORTS i NOT_SUPPORTED dopasowują się do wołającego")
    void mandatoryNeverSupportsNotSupported() {
        assertThatThrownBy(() -> auditService.logMandatory("bez transakcji"))
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessage("No existing transaction found for transaction marked with propagation 'mandatory'");
        orderService.placeOrderWithMandatoryAudit("Kawa ziarnista 1kg", 1);
        assertThat(auditEntries()).isEqualTo(1);

        assertThatThrownBy(() -> orderService.callNeverInsideTransaction())
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessage("Existing transaction found for transaction marked with propagation 'never'");
        assertThat(auditService.neverSeesTransaction()).isFalse();

        assertThat(auditService.supportsSeesTransaction()).as("SUPPORTS bez transakcji").isFalse();
        assertThat(orderService.probesInsideTransaction()).as("[SUPPORTS, NOT_SUPPORTED] w transakcji")
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("5. NESTED z JpaTransactionManager: wewnątrz transakcji → NestedTransactionNotSupportedException")
    void nestedWithJpa() {
        assertThatThrownBy(() -> orderService.placeOrderThenFail("Kawa ziarnista 1kg", 1, Propagation.NESTED))
                .isInstanceOf(NestedTransactionNotSupportedException.class)
                .hasMessage("Transaction manager does not allow nested transactions by default - "
                        + "specify 'nestedTransactionAllowed' property with value 'true'");
        assertThat(orders()).isZero();

        auditService.logNested("bez transakcji wołającego");
        assertThat(auditEntries()).as("bez transakcji zewnętrznej NESTED działa jak REQUIRED").isEqualTo(1);
    }

    @Test
    @DisplayName("5. NESTED z JPA nawet po setNestedTransactionAllowed(true): brak punktów zapisu w HibernateJpaDialect")
    void nestedAllowedStillNotSupportedWithJpa() {
        JpaTransactionManager jpaTransactions = new JpaTransactionManager(entityManagerFactory);
        jpaTransactions.setNestedTransactionAllowed(true);                            // „pozwól na NESTED”
        TransactionTemplate outer = new TransactionTemplate(jpaTransactions);
        TransactionTemplate nested = new TransactionTemplate(jpaTransactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            jdbc.sql("insert into orders (product, quantity) values ('Kawa ziarnista 1kg', 1)").update();
            nested.executeWithoutResult(inner -> jdbc.sql("insert into audit_log (message) values ('x')").update());
        })).isInstanceOf(NestedTransactionNotSupportedException.class)
                .hasMessage("JpaDialect does not support savepoints - check your JPA provider's capabilities");

        assertThat(orders()).as("cała transakcja wycofana").isZero();
    }

    @Test
    @DisplayName("5. NESTED z menedżerem JDBC: punkt zapisu cofa tylko część wewnętrzną")
    void nestedWithJdbcSavepoint() {
        var jdbcTransactions = new JdbcTransactionManager(dataSource);
        var outer = new TransactionTemplate(jdbcTransactions);
        var nested = new TransactionTemplate(jdbcTransactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

        outer.executeWithoutResult(status -> {
            jdbc.sql("insert into orders (product, quantity) values ('Kawa ziarnista 1kg', 1)").update();
            nested.executeWithoutResult(inner -> {
                jdbc.sql("insert into audit_log (message) values ('wpis do cofnięcia')").update();
                inner.setRollbackOnly();          // cofnij do punktu zapisu (savepoint)
            });
        });

        assertThat(orders()).as("część zewnętrzna zatwierdzona").isEqualTo(1);
        assertThat(auditEntries()).as("część zagnieżdżona cofnięta do punktu zapisu").isZero();
    }
}
