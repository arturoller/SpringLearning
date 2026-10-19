package s11_testing.solutions.test06_testcontainers;

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
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import s11_testing.test06_testcontainers.CustomerImporter;
import s11_testing.test06_testcontainers.CustomerRepository;
import s11_testing.test06_testcontainers.NewCustomer;
import s11_testing.test06_testcontainers.Test06Application;
import s11_testing.test06_testcontainers.Test06Containers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Test06Testcontainers. {@code @Tag("wzorzec")} — biegną zawsze (klasy
 * z kontenerami bez Dockera są pomijane). Konfigurację aplikacji wskazujemy jawnie, bo pakiet {@code solutions…}
 * nie leży pod pakietem lekcji; rozwiązanie ćwiczenia 2 ({@link SafeCustomerImporterSolution}) dołączamy do classes.
 */
@Tag("wzorzec")
class Test06TestcontainersSolutionsTest {

    static final List<NewCustomer> IMPORT = List.of(
            new NewCustomer("Jan Kowalski (duplikat)", "Warszawa", "jan@example.com", false),
            new NewCustomer("Krzysztof Wrona", "Lublin", "krzysztof@example.com", false),
            new NewCustomer("Anna Bez Maila", "Opole", null, false));

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    @SpringBootTest(classes = Test06Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test06")
    class Solution1 {

        @Container
        @ServiceConnection
        static PostgreSQLContainer postgres = new PostgreSQLContainer(Test06Containers.POSTGRES_IMAGE);

        @Autowired
        CustomerRepository repository;

        @Test
        @DisplayName("Wzorzec 1: własny kontener PostgreSQL z @ServiceConnection")
        void solution1() {
            assertThat(repository.databaseVersion()).startsWith("PostgreSQL 18");
            assertThat(repository.count()).isEqualTo(7);
        }
    }

    @Nested
    @SpringBootTest(classes = {Test06Application.class, SafeCustomerImporterSolution.class},
            webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test06")
    class Solution2OnH2 {

        @Autowired
        SafeCustomerImporterSolution importer;

        @Autowired
        JdbcClient jdbc;

        @AfterEach
        void cleanUp() {
            jdbc.sql("DELETE FROM customer WHERE id > 7").update();
        }

        @Test
        @DisplayName("Wzorzec 2 (H2): bezpieczny import pomija duplikat, zapisuje dwóch nowych")
        void solution2OnH2() {
            assertThat(importer.importAll(IMPORT)).isEqualTo(new CustomerImporter.ImportResult(2, 1));
        }
    }

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    @SpringBootTest(classes = {Test06Application.class, SafeCustomerImporterSolution.class},
            webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test06")
    class Solution2OnPostgres {

        @Container
        @ServiceConnection
        static PostgreSQLContainer postgres = new PostgreSQLContainer(Test06Containers.POSTGRES_IMAGE);

        @Autowired
        SafeCustomerImporterSolution importer;

        @Autowired
        CustomerRepository repository;

        @Test
        @DisplayName("Wzorzec 2 (PostgreSQL): ten sam import działa na prawdziwej bazie")
        void solution2OnPostgres() {
            assertThat(importer.importAll(IMPORT)).isEqualTo(new CustomerImporter.ImportResult(2, 1));
            assertThat(repository.count()).isEqualTo(9);
        }
    }

    @Nested
    @Testcontainers(disabledWithoutDocker = true)
    @JdbcTest
    @ActiveProfiles("test06")
    @ContextConfiguration(classes = Test06Application.class)
    @Import({Test06Containers.class, CustomerRepository.class})
    class Solution3 {

        @Autowired
        CustomerRepository repository;

        @Test
        @DisplayName("Wzorzec 3: @JdbcTest na PostgreSQL z kontenera-beana")
        void solution3() {
            assertThat(repository.databaseVersion()).startsWith("PostgreSQL 18");
        }
    }
}
