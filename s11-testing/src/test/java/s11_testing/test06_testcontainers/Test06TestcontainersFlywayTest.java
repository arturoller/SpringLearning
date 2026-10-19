package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestContextManager;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 4 lekcji Test06Testcontainers — PUŁAPKA: Flyway na PostgreSQL bez modułu {@code flyway-database-postgresql}.
 * Od Flyway 10 obsługa PostgreSQL (i większości baz) jest w OSOBNYCH modułach; {@code spring-boot-starter-flyway}
 * daje tylko {@code flyway-core}. Moduł s11-testing celowo nie ma tej zależności — test pokazuje, co wtedy widać.
 * Kontekst budujemy przez TestContextManager (klasa-nośnik adnotacji bez metod testowych).
 */
@Testcontainers(disabledWithoutDocker = true)   // tylko warunek „pomiń bez Dockera”
class Test06TestcontainersFlywayTest {

    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration/test04"})
    @Import(Test06Containers.class)
    static class FlywayOnPostgres {
    }

    @Test
    @DisplayName("4. PUŁAPKA: Flyway 12 + PostgreSQL bez flyway-database-postgresql → „Unsupported Database: PostgreSQL 18…”")
    void flywayWithoutPostgresModuleFails() {
        assertThatThrownBy(() -> new TestContextManager(FlywayOnPostgres.class).getTestContext().getApplicationContext())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Failed to load ApplicationContext")
                .rootCause()
                .hasMessageStartingWith("Unsupported Database: PostgreSQL 18");
    }
}
