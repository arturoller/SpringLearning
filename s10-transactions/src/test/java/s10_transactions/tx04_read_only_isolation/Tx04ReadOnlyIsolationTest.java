package s10_transactions.tx04_read_only_isolation;

import org.hibernate.FlushMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx04ReadOnlyIsolation — „WYNIK” każdej sekcji.
 *
 * <p>Jak w poprzednich lekcjach: {@code @SpringBootTest} BEZ transakcji testu, stan bazy czytany JdbcClientem,
 * dane przywracane przed każdym testem.</p>
 *
 * <p>Współbieżność BEZ wątków: „drugi użytkownik” to {@link SecondUser} — osobne połączenie JDBC z własną transakcją.
 * Jego kroki wykonujemy w ściśle określonym miejscu: przed wywołaniem serwisu albo w parametrze {@code meanwhile}
 * („w międzyczasie”), który serwis uruchamia między swoim odczytem a zapisem. Kolejność kroków jest zawsze ta sama,
 * więc wynik też — bez {@code sleep} i bez zgadywania, który wątek zdąży pierwszy.</p>
 *
 * <p>Komunikaty błędów H2 są tłumaczone na język systemu (na polskim Windowsie: „Wykryto zakleszczenie…”), dlatego
 * sprawdzamy typ wyjątku i KOD błędu bazy, a nie treść komunikatu.</p>
 */
@SpringBootTest
@ActiveProfiles("tx04")
class Tx04ReadOnlyIsolationTest {

    @Autowired
    ReportService reports;

    @Autowired
    StockReader reader;

    @Autowired
    StockService stockService;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DataSource dataSource;

    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetProducts() {
        jdbc.sql("delete from products").update();
        jdbc.sql("""
                insert into products (sku, name, category, stock) values
                  ('ELE-001', 'Laptop Pro 14', 'ELEKTRONIKA', 7),
                  ('ELE-002', 'Smartfon X', 'ELEKTRONIKA', 0),
                  ('ELE-004', 'Monitor 27 cali', 'ELEKTRONIKA', 4),
                  ('DOM-001', 'Ekspres do kawy', 'DOM', 2),
                  ('DOM-002', 'Lampka biurkowa', 'DOM', 18)""").update();
        jdbc.sql("delete from versioned_products").update();
        jdbc.sql("""
                insert into versioned_products (sku, name, stock, version) values
                  ('ELE-001', 'Laptop Pro 14', 7, 0),
                  ('DOM-001', 'Ekspres do kawy', 2, 0)""").update();
    }

    int stock(String sku) {
        return jdbc.sql("select stock from products where sku = ?").param(sku).query(Integer.class).single();
    }

    /** Kod błędu bazy z pierwszego SQLException w łańcuchu przyczyn (komunikaty H2 zależą od języka, kody — nie). */
    static int sqlErrorCode(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlError) {
                return sqlError.getErrorCode();
            }
        }
        throw new AssertionError("Brak SQLException w łańcuchu przyczyn: " + error);
    }

    /** Drugi użytkownik sprzedaje 1 sztukę i od razu zatwierdza. */
    static Runnable sellsOne(SecondUser user, String sku) {
        return () -> user.updateAndCommit("update products set stock = stock - 1 where sku = ?", sku);
    }

    @Test
    @DisplayName("1. readOnly pod maską: FlushMode.MANUAL, sesja tylko do odczytu; SQL UPDATE w H2 i tak przechodzi")
    void readOnlyUnderTheHood() {
        assertThat(reports.insideReadOnly()).isEqualTo(
                new ReportService.TransactionInsight(true, FlushMode.MANUAL, true));
        assertThat(reports.insideReadWrite()).isEqualTo(
                new ReportService.TransactionInsight(false, FlushMode.AUTO, false));

        // Spring Data: cała klasa repozytorium tylko do odczytu, metody zapisujące — zwykła transakcja.
        assertThat(SimpleJpaRepository.class.getAnnotation(Transactional.class).readOnly()).isTrue();

        // readOnly to NIE zakaz zapisu: H2 ignoruje Connection.setReadOnly(true), więc ręczny UPDATE przechodzi.
        assertThat(reports.sqlUpdateInReadOnlyTransaction("DOM-002", 17)).isEqualTo(1);
        assertThat(stock("DOM-002")).isEqualTo(17);
    }

    @Test
    @DisplayName("2. PUŁAPKA: zapis encji w transakcji readOnly po cichu przepada (też z save() i przez inny bean)")
    void writesInReadOnlyAreSilentlyLost() {
        assertThat(reports.sellInReadOnlyTransaction("ELE-001", 1)).as("obiekt w pamięci: 7 - 1").isEqualTo(6);
        assertThat(stock("ELE-001")).as("w bazie bez zmian").isEqualTo(7);

        reports.sellAndSaveInReadOnlyTransaction("ELE-001", 1);
        assertThat(stock("ELE-001")).as("jawne save() też nie pomaga").isEqualTo(7);

        reports.reportThatSells("ELE-004", 1);
        assertThat(stock("ELE-004")).as("StockService.sell dołączył do transakcji tylko do odczytu").isEqualTo(4);
    }

    @Test
    @DisplayName("3. Brudny odczyt: READ_UNCOMMITTED widzi niezatwierdzoną zmianę, READ_COMMITTED — nie")
    void dirtyRead() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            bob.update("update products set stock = 0 where sku = ?", "ELE-001");   // BEZ commit

            assertThat(reader.readStockReadUncommitted("ELE-001")).as("brudny odczyt").isZero();
            assertThat(reader.readStockReadCommitted("ELE-001")).as("tylko zatwierdzone").isEqualTo(7);

            bob.rollback();                                                          // „zero” nigdy nie istniało
        }
        assertThat(stock("ELE-001")).isEqualTo(7);
    }

    @Test
    @DisplayName("3. Niepowtarzalny odczyt: READ_COMMITTED widzi cudzy commit w połowie, REPEATABLE_READ — nie")
    void nonRepeatableRead() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            assertThat(reader.readStockTwiceReadCommitted("ELE-001", sellsOne(bob, "ELE-001")))
                    .containsExactly(7, 6);
            assertThat(reader.readStockTwiceRepeatableRead("ELE-001", sellsOne(bob, "ELE-001")))
                    .as("ten sam wynik przez całą transakcję").containsExactly(6, 6);
        }
        assertThat(stock("ELE-001")).as("obie sprzedaże drugiego użytkownika są w bazie").isEqualTo(5);
    }

    @Test
    @DisplayName("3. Fantom: READ_COMMITTED widzi nowy wiersz, REPEATABLE_READ w H2 — nie")
    void phantomRead() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            assertThat(reader.countTwiceReadCommitted("DOM", () -> bob.updateAndCommit(
                    "insert into products (sku, name, category, stock) values ('DOM-003', 'Czajnik', 'DOM', 5)")))
                    .containsExactly(2L, 3L);
            assertThat(reader.countTwiceRepeatableRead("DOM", () -> bob.updateAndCommit(
                    "insert into products (sku, name, category, stock) values ('DOM-004', 'Toster', 'DOM', 3)")))
                    .containsExactly(3L, 3L);
        }
    }

    @Test
    @DisplayName("3. Isolation.DEFAULT = poziom bazy (H2: READ COMMITTED); Spring ustawia i przywraca poziom połączenia")
    void defaultIsolationAndReset() {
        assertThat(reader.connectionIsolationDefault()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
        assertThat(reader.connectionIsolationSerializable()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
        assertThat(reader.connectionIsolationDefault()).as("po transakcji poziom przywrócony")
                .isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
    }

    @Test
    @DisplayName("4. Kontekst utrwalania: drugi findById zwraca STARY stan encji, SQL widzi nowy; refresh pomaga")
    void persistenceContextActsLikeRepeatableRead() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            assertThat(reader.readEntityTwice("ELE-001", sellsOne(bob, "ELE-001")))
                    .as("[encja 1. odczyt, encja 2. odczyt, SQL, encja po refresh]").containsExactly(7, 7, 6, 6);
        }
    }

    @Test
    @DisplayName("5. Utracona aktualizacja: dwie sprzedaże po 1 szt., a stan spadł tylko o 1; atomowy UPDATE — o 2")
    void lostUpdateAndAtomicUpdate() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            stockService.sellWithPause("ELE-001", 1, sellsOne(bob, "ELE-001"));
        }
        assertThat(stock("ELE-001")).as("7 - 1 - 1 powinno dać 5").isEqualTo(6);

        resetProducts();
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            sellsOne(bob, "ELE-001").run();
            assertThat(stockService.sellAtomically("ELE-001", 1)).isTrue();
        }
        assertThat(stock("ELE-001")).isEqualTo(5);
        assertThat(stockService.sellAtomically("ELE-001", 99)).as("za mało — 0 wierszy").isFalse();
        assertThat(stock("ELE-001")).isEqualTo(5);
    }

    @Test
    @DisplayName("5. @Version: cudza zmiana w międzyczasie → ObjectOptimisticLockingFailureException, nic nie nadpisane")
    void optimisticLocking() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            assertThatThrownBy(() -> stockService.sellVersioned("ELE-001", 1, () -> bob.updateAndCommit(
                    "update versioned_products set stock = stock - 1, version = version + 1 where sku = ?", "ELE-001")))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("Unexpected row count (expected row count 1 but was 0)")
                    .hasMessageContaining("where sku=? and version=?");
        }
        assertThat(jdbc.sql("select stock || '/' || version from versioned_products where sku = 'ELE-001'")
                .query(String.class).single()).as("stan/wersja: zostaje sprzedaż drugiego użytkownika").isEqualTo("6/1");
    }

    @Test
    @DisplayName("5. REPEATABLE_READ w H2: nadpisanie wiersza zmienionego w międzyczasie → wycofanie (błąd 40001)")
    void repeatableReadRejectsLostUpdateInH2() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            assertThatThrownBy(() -> stockService.sellWithPauseRepeatableRead("ELE-001", 1, sellsOne(bob, "ELE-001")))
                    .isInstanceOf(CannotAcquireLockException.class)
                    .satisfies(error -> assertThat(sqlErrorCode(error)).as("kod H2: wycofanie transakcji")
                            .isEqualTo(40001));
        }
        assertThat(stock("ELE-001")).as("zostaje tylko sprzedaż drugiego użytkownika").isEqualTo(6);
    }

    @Test
    @DisplayName("5. PESSIMISTIC_WRITE: wiersz zablokowany od odczytu — drugi użytkownik nie może go zmienić")
    void pessimisticLocking() {
        AtomicReference<RuntimeException> bobsProblem = new AtomicReference<>();
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            stockService.sellWithPessimisticLock("ELE-001", 1, () -> {
                try {
                    sellsOne(bob, "ELE-001").run();       // czeka na naszą blokadę… w tym samym wątku → limit H2 (2 s)
                } catch (IllegalStateException e) {
                    bobsProblem.set(e);
                }
            });
        }
        assertThat(bobsProblem.get()).as("drugi użytkownik nie przeszedł przez blokadę").isNotNull();
        assertThat(sqlErrorCode(bobsProblem.get())).as("kod H2: limit czasu blokady").isEqualTo(50200);
        assertThat(stock("ELE-001")).as("nasza sprzedaż zapisana, nic nie nadpisane").isEqualTo(6);
    }

    @Test
    @DisplayName("6. timeout = 5: Spring ustawia instrukcjom JDBC limit równy czasowi, który transakcji jeszcze został")
    void timeoutBecomesQueryTimeout() {
        assertThat(reader.queryTimeoutWithTransactionTimeout()).isBetween(1, 5);
    }

    @Test
    @DisplayName("6. timeout = 1: czekania na blokadę (2 s) nikt nie przerywa; termin wychodzi przy NASTĘPNYM zapytaniu")
    void timeoutDetectedAtNextStatement() {
        try (SecondUser bob = SecondUser.connect(dataSource)) {
            bob.update("update products set stock = 3 where sku = ?", "ELE-001");    // blokada, bez commit

            assertThatThrownBy(() -> stockService.sellWithTimeout("ELE-001", 1))
                    .isInstanceOf(TransactionTimedOutException.class)
                    .hasMessageStartingWith("Transaction timed out: deadline was ");

            bob.rollback();
        }
        assertThat(stock("ELE-001")).isEqualTo(7);
    }

    @Test
    @DisplayName("7. Dołączenie (REQUIRED) do istniejącej transakcji: isolation i readOnly metody wołanej są ignorowane")
    void attributesIgnoredWhenJoining() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);   // zwykła transakcja, DEFAULT

        Integer isolationInside = outer.execute(status -> reader.connectionIsolationSerializable());
        assertThat(isolationInside).as("SERIALIZABLE zignorowane — poziom transakcji zewnętrznej")
                .isEqualTo(Connection.TRANSACTION_READ_COMMITTED);

        outer.executeWithoutResult(status -> reports.sellInReadOnlyTransaction("ELE-001", 1));
        assertThat(stock("ELE-001")).as("readOnly zignorowane — sprzedaż ZAPISANA").isEqualTo(6);
    }
}
