package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1–3 lekcji Test06Testcontainers: prawdziwy PostgreSQL w kontenerze Dockera na czas testów tej klasy.
 * Bez Dockera klasa jest POMIJANA ({@code disabledWithoutDocker = true}), więc {@code ./mvnw test} przechodzi u każdego.
 */
@Testcontainers(disabledWithoutDocker = true)   // rozszerzenie JUnit: uruchamia i zatrzymuje pola @Container
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test06")
class Test06TestcontainersPostgresTest {

    @Container           // static = JEDEN kontener na całą klasę (start przed pierwszym testem, stop po ostatnim)
    @ServiceConnection   // Boot sam tworzy DataSource do tego kontenera — bez spring.datasource.url
    static PostgreSQLContainer postgres = new PostgreSQLContainer(Test06Containers.POSTGRES_IMAGE);

    @Autowired
    ApplicationContext context;

    @Autowired
    CustomerRepository repository;

    @Autowired
    CustomerImporter importer;

    @Test
    @DisplayName("1. @ServiceConnection: aplikacja rozmawia z PostgreSQL 18 w kontenerze (losowy port), schemat i dane wczytane")
    void connectedToPostgresInContainer() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(repository.databaseVersion()).startsWith("PostgreSQL 18");
        assertThat(context.getBean(JdbcConnectionDetails.class).getJdbcUrl()).isEqualTo(postgres.getJdbcUrl());
        assertThat(postgres.getJdbcUrl()).startsWith("jdbc:postgresql://localhost:");
        assertThat(repository.count()).isEqualTo(7);
    }

    @Test
    @DisplayName("2. Kontener statyczny: wszystkie testy klasy dzielą TEN SAM kontener (ten sam port)")
    void staticContainerIsShared() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(context.getBean(JdbcConnectionDetails.class).getJdbcUrl())   // np. jdbc:postgresql://localhost:61316/test?loggerLevel=OFF
                .contains("localhost:" + postgres.getMappedPort(5432) + "/test");
    }

    @Test
    @DisplayName("3. PostgreSQL: po błędzie UNIQUE transakcja jest PRZERWANA — import „pomiń duplikat i jedź dalej” pada")
    void abortedTransactionOnPostgres() {
        var customers = List.of(
                new NewCustomer("Jan Kowalski (duplikat)", "Warszawa", "jan@example.com", false),
                new NewCustomer("Krzysztof Wrona", "Lublin", "krzysztof@example.com", false));

        assertThatThrownBy(() -> importer.importAll(customers))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("SQL state [25P02]")
                .hasMessageContaining("ERROR: current transaction is aborted, commands ignored until end of transaction block");
        assertThat(repository.count()).as("cała transakcja wycofana — nikt nie został dodany").isEqualTo(7);
    }
}
