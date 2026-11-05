package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test06Testcontainers: druga klasa z WSPÓLNYM kontenerem — inna konfiguracja
 * ({@code @SpringBootTest}, więc inny kontekst niż w Test06SharedContainerJpaTest), ten sam PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test06")
@ImportTestcontainers(SharedPostgres.class)
@Testcontainers(disabledWithoutDocker = true)
class Test06SharedContainerAppTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    ProductService service;

    @Test
    @DisplayName("Sekcja 4: @SpringBootTest używa TEGO SAMEGO kontenera co @DataJpaTest — adres JDBC jest identyczny")
    void usesSameContainer() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).isEqualTo(SharedPostgres.POSTGRES.getJdbcUrl());
        }
        assertThat(SharedPostgres.POSTGRES.isRunning()).isTrue();
        assertThat(service.mostExpensive(1)).extracting(Product::getName).containsExactly("Czajnik");
    }
}
