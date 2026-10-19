package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.TestTransaction;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1–4 i 6 lekcji Test04DataJpaTest: wycinek JPA od środka. Dane startowe (14 produktów) wstawia migracja
 * Flyway V2; każdy test zmienia je tylko w swojej transakcji, a Spring wycofuje ją po teście — dlatego kolejność
 * testów nie ma znaczenia.
 */
@DataJpaTest
@ActiveProfiles("test04")   // profil lekcji: Flyway z db/migration/test04, ddl-auto=validate
class Test04DataJpaTestRepositoryTest {

    @Autowired
    ProductRepository repository;

    @Autowired
    TestEntityManager entityManager;   // TestEntityManager = pomocnik testowy wokół EntityManager (JPA)

    @Autowired
    JdbcClient jdbc;                   // zwykłe SQL — do sprawdzania tabeli „obok” JPA

    @Test
    @DisplayName("1. Każdy test działa w transakcji, którą Spring po teście WYCOFUJE (rollback)")
    void transactionIsRolledBack() {
        assertThat(TestTransaction.isActive()).isTrue();
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();

        repository.save(new Product("DOM-003", "Odkurzacz", "DOM", new BigDecimal("599.00"), 3));
        assertThat(repository.count()).isEqualTo(15);

        TestTransaction.end();   // to samo, co Spring robi po teście: koniec transakcji = wycofanie

        assertThat(TestTransaction.isActive()).isFalse();
        assertThat(repository.count()).as("odkurzacza już nie ma — wycofany").isEqualTo(14);
    }

    @Test
    @DisplayName("2. TestEntityManager przygotowuje dane, repozytorium je znajduje")
    void testEntityManagerPreparesData() {
        Long id = entityManager.persistAndGetId(
                new Product("ODZ-003", "Czapka zimowa", "ODZIEZ", new BigDecimal("39.99"), 40), Long.class);
        entityManager.flush();
        entityManager.clear();

        Product found = repository.findBySku("ODZ-003").orElseThrow();

        assertThat(found.getId()).isEqualTo(id);
        assertThat(repository.findByCategoryOrderBySku("ODZIEZ"))
                .extracting(Product::getSku)
                .containsExactly("ODZ-001", "ODZ-002", "ODZ-003");
    }

    @Test
    @DisplayName("3. Bez clear(): repozytorium oddaje TEN SAM obiekt z kontekstu utrwalania; po flush()+clear() — nowy z bazy")
    void flushAndClearForceRealRead() {
        Product first = repository.findBySku("SPO-001").orElseThrow();

        assertThat(repository.findById(first.getId()).orElseThrow())
                .as("z pamięci kontekstu utrwalania (pierwszy poziom pamięci podręcznej)")
                .isSameAs(first);

        entityManager.flush();
        entityManager.clear();

        Product reloaded = repository.findById(first.getId()).orElseThrow();
        assertThat(reloaded).isNotSameAs(first);
        assertThat(reloaded.getName()).isEqualTo("Kawa ziarnista 1kg");
    }

    @Test
    @DisplayName("3. Zwykłe SQL (JdbcClient) omija kontekst utrwalania: bez flush() widzi STARĄ wartość, po flush() — nową")
    void plainSqlSeesChangesOnlyAfterFlush() {
        Product lamp = repository.findBySku("DOM-002").orElseThrow();
        lamp.setStock(0);   // zmiana tylko w pamięci (kontekst utrwalania) — UPDATE jeszcze nie poszedł

        String sql = "SELECT stock FROM product WHERE sku = 'DOM-002'";
        assertThat(jdbc.sql(sql).query(Integer.class).single()).as("przed flush").isEqualTo(18);

        entityManager.flush();   // teraz Hibernate wysyła UPDATE (w tej samej transakcji testu)

        assertThat(jdbc.sql(sql).query(Integer.class).single()).as("po flush").isZero();
    }

    @Test
    @DisplayName("3. PUŁAPKA: zmiana łamiąca NOT NULL „przechodzi”, dopóki nikt nie zrobi flush()")
    void constraintCheckedOnlyAtFlush() {
        Product coffee = repository.findBySku("SPO-001").orElseThrow();
        coffee.setName(null);   // kolumna name jest NOT NULL w bazie

        // Bez flush: żadnego UPDATE, żadnego błędu. findById bierze obiekt z pamięci — z nazwą null.
        assertThat(repository.findById(coffee.getId()).orElseThrow().getName()).isNull();

        // Dopiero flush wysyła UPDATE — baza odrzuca null (SQLState 23502 = naruszenie NOT NULL w standardzie SQL).
        assertThatThrownBy(repository::flush)
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23502"));
    }

    @Test
    @DisplayName("3. Zapytanie JPQL na tej samej tabeli samo robi flush (FlushMode AUTO) — błąd wychodzi przy odczycie")
    void jpqlQueryTriggersAutoFlush() {
        Product coffee = repository.findBySku("SPO-001").orElseThrow();
        coffee.setName(null);

        // findBySku to zapytanie do tabeli product: Hibernate najpierw wysyła zaległy UPDATE, żeby wynik był aktualny.
        assertThatThrownBy(() -> repository.findBySku("SPO-001"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .isInstanceOfSatisfying(SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo("23502"));
    }

    @Test
    @DisplayName("4. @Sql: skrypt dodaje 2 produkty w transakcji testu — repozytorium je widzi")
    @Sql("/db/test04/clearance-sale.sql")
    void sqlScriptInsideTestTransaction() {
        assertThat(repository.count()).isEqualTo(16);
        assertThat(repository.findLowStock(5))
                .extracting(Product::getSku)
                .containsExactly("ELE-002", "SPO-003", "DOM-001", "DOM-003", "KSI-003", "ELE-004");
    }

    @Test
    @DisplayName("4. @Sql(statements = ...): krótka zmiana danych tylko dla tego testu")
    @Sql(statements = "UPDATE product SET stock = 0 WHERE sku = 'KSI-001'")
    void sqlStatements() {
        assertThat(repository.findBySku("KSI-001").orElseThrow().getStock()).isZero();
    }

    @Test
    @DisplayName("6. Flyway działa w wycinku: migracje V1 i V2 w historii, 14 produktów z V2")
    void flywayRunsInSlice() {
        // Flyway 12 zapisuje też wiersz o utworzeniu samej tabeli historii (typ TABLE) — liczymy tylko migracje SQL.
        var versions = jdbc.sql("""
                        SELECT "version" FROM "flyway_schema_history"
                        WHERE "type" = 'SQL' AND "success" ORDER BY "installed_rank"
                        """)
                .query(String.class)
                .list();

        assertThat(versions).containsExactly("1", "2");
        assertThat(repository.count()).isEqualTo(14);
    }
}
