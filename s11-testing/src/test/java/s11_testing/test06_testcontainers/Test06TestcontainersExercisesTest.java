package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Test06Testcontainers. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Klasy z kontenerami bez Dockera są POMIJANE. Uwaga: gdy {@code todo()} stoi w inicjalizacji pola statycznego,
 * test pada przy ładowaniu klasy (ExceptionInInitializerError), a przyczyną jest {@code UnsupportedOperationException: TODO}.
 */
@Tag("cwiczenie")
class Test06TestcontainersExercisesTest {

    /** Znacznik „tu jeszcze nic nie ma” — rzuca wyjątek, dopóki go nie zastąpisz swoim kodem. */
    static <T> T todo() {
        throw new UnsupportedOperationException("TODO");
    }

    static final List<NewCustomer> IMPORT = List.of(
            new NewCustomer("Jan Kowalski (duplikat)", "Warszawa", "jan@example.com", false),
            new NewCustomer("Krzysztof Wrona", "Lublin", "krzysztof@example.com", false),
            new NewCustomer("Anna Bez Maila", "Opole", null, false));

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test06")
    class Exercise1 {

        // TODO (ćwiczenie 1): utwórz kontener PostgreSQL z obrazu Test06Containers.POSTGRES_IMAGE
        @Container
        @ServiceConnection
        static PostgreSQLContainer postgres = todo();

        @Autowired
        CustomerRepository repository;

        @Test
        @DisplayName("Ćwiczenie 1: własny kontener PostgreSQL z @ServiceConnection")
        void exercise1() {
            assertThat(repository.databaseVersion()).startsWith("PostgreSQL 18");
            assertThat(repository.count()).isEqualTo(7);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test06")
    class Exercise2OnH2 {

        @Autowired
        SafeCustomerImporter importer;

        @Autowired
        JdbcClient jdbc;

        @AfterEach
        void cleanUp() {   // import zatwierdza transakcję, a baza H2 kontekstu jest współdzielona — sprzątamy
            jdbc.sql("DELETE FROM customer WHERE id > 7").update();
        }

        @Test
        @DisplayName("Ćwiczenie 2 (H2): bezpieczny import pomija duplikat, zapisuje dwóch nowych")
        void exercise2OnH2() {
            assertThat(importer.importAll(IMPORT)).isEqualTo(new CustomerImporter.ImportResult(2, 1));
        }
    }

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test06")
    class Exercise2OnPostgres {

        @Container
        @ServiceConnection
        static PostgreSQLContainer postgres = new PostgreSQLContainer(Test06Containers.POSTGRES_IMAGE);

        @Autowired
        SafeCustomerImporter importer;

        @Autowired
        CustomerRepository repository;

        @Test
        @DisplayName("Ćwiczenie 2 (PostgreSQL): ten sam import działa na prawdziwej bazie")
        void exercise2OnPostgres() {
            assertThat(importer.importAll(IMPORT)).isEqualTo(new CustomerImporter.ImportResult(2, 1));
            assertThat(repository.count()).isEqualTo(9);
        }
    }

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    @JdbcTest
    @ActiveProfiles("test06")
    // TODO (ćwiczenie 3): dołącz do kontekstu konfigurację z kontenerem-beanem i repozytorium klientów (@Import)
    class Exercise3 {

        @Autowired(required = false)
        CustomerRepository repository;

        @Test
        @DisplayName("Ćwiczenie 3: @JdbcTest na PostgreSQL z kontenera-beana")
        void exercise3() {
            todo();   // TODO: usuń tę linię po dodaniu adnotacji

            assertThat(repository.databaseVersion()).startsWith("PostgreSQL 18");
        }
    }
}
