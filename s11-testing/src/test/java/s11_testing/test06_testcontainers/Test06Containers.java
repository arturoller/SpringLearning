package s11_testing.test06_testcontainers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Lekcja Test06Testcontainers, sekcja 5: kontener jako BEAN Springa. Spring uruchamia kontener, gdy tworzy bean,
 * i zatrzymuje go przy zamknięciu kontekstu. {@code @ServiceConnection} na metodzie {@code @Bean} działa tak samo jak
 * na polu: Boot tworzy z kontenera dane połączenia (JdbcConnectionDetails) i z nich DataSource.
 * Używają jej: testy wycinkowe tej lekcji ({@code @Import(Test06Containers.class)}) i Test06DevApplication
 * (uruchamianie aplikacji lokalnie z prawdziwym PostgreSQL).
 */
@TestConfiguration(proxyBeanMethods = false)   // bez proxy metod — metody @Bean nie wołają się nawzajem
public class Test06Containers {

    /** Obraz z JAWNYM tagiem (nigdy „latest” — wtedy test mógłby jutro dostać inną wersję bazy). */
    public static final String POSTGRES_IMAGE = "postgres:18-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
