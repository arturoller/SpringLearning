package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test04DataJpaTest: {@code @AutoConfigureTestDatabase(replace = NONE)} — wycinek NIE podmienia bazy
 * na wbudowaną z losową nazwą, tylko używa tej z konfiguracji (tu: {@code jdbc:h2:mem:test04} z profilu lekcji).
 * To zapowiedź Test06Testcontainers: tym samym sposobem test dostanie PRAWDZIWY PostgreSQL z kontenera
 * (adres poda {@code @ServiceConnection}), a nie H2.
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // replace = zastąp; NONE = nie zastępuj
@ActiveProfiles("test04")
class Test04RealDatabaseTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("Sekcja 4: z replace = NONE baza to ta z konfiguracji (jdbc:h2:mem:test04), z danymi z Flyway")
    void configuredDatabaseIsUsed() throws Exception {
        String url;
        try (var connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        }

        assertThat(url).startsWith("jdbc:h2:mem:test04");
        assertThat(repository.count()).isEqualTo(5);
    }
}
