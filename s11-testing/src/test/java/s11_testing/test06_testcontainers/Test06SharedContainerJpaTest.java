package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test06Testcontainers: kontener WSPÓŁDZIELONY przez różne konfiguracje testów. Ta klasa
 * ({@code @DataJpaTest}) i Test06SharedContainerAppTest ({@code @SpringBootTest}) mają RÓŻNE konteksty, ale ten sam
 * kontener ze statycznego pola {@link SharedPostgres#POSTGRES} — jeden start Dockera zamiast dwóch.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test06")
@ImportTestcontainers(SharedPostgres.class) // import testcontainers = dołącz kontenery z pól statycznych tej klasy
@Testcontainers(disabledWithoutDocker = true)
class Test06SharedContainerJpaTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("Sekcja 4: @DataJpaTest używa WSPÓLNEGO kontenera SharedPostgres.POSTGRES")
    void usesSharedContainer() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).isEqualTo(SharedPostgres.POSTGRES.getJdbcUrl());
        }
        assertThat(repository.count()).isEqualTo(4);
    }
}
