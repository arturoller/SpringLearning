package s11_testing.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Test07DockerCompose — PUŁAPKA {@code skip.in-tests}: profil test07 WŁĄCZA Docker Compose, ale
 * w testach Boot domyślnie go POMIJA ({@code spring.docker.compose.skip.in-tests=true}). Compose nie startuje, adresu
 * bazy nikt nie podał, więc Boot tworzy wbudowaną H2 — a migracje (zgodne z H2) i tak przechodzą. Test „działa”, ale
 * NIE na PostgreSQL z compose. Ten test nie potrzebuje Dockera — biegnie zawsze.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test07")
class Test07SkipInTestsTest {

    @Autowired
    Environment environment;

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("Sekcja 2, PUŁAPKA: compose włączony w profilu, ale w teście pominięty — aplikacja po cichu działa na wbudowanej H2")
    void composeSkippedInTests() throws Exception {
        assertThat(environment.getProperty("spring.docker.compose.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("spring.docker.compose.skip.in-tests")).isNull(); // nie ustawione = domyślnie true

        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
        }
    }
}
