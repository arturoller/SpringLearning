package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Jdbc05Repository: {@code replace = NONE} zostawia bazę z konfiguracji aplikacji
 * (adres z application-jdbc05.properties), zamiast podmieniać ją na świeżą wbudowaną.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductRepository.class)
@ActiveProfiles("jdbc05")
class Jdbc05RepositoryRealDatabaseTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("5. Z replace = NONE test używa bazy jdbc:h2:mem:jdbc05 z ustawień lekcji")
    void section5() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:jdbc05");
        }
        assertThat(repository.count()).isEqualTo(14);   // migracje Flyway wykonały się na tej bazie
    }
}
