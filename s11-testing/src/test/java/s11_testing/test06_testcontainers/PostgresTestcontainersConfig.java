package s11_testing.test06_testcontainers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Kontener PostgreSQL jako BEAN (sekcja 4 lekcji Test06Testcontainers). Spring tworzy go razem z kontekstem,
 * uruchamia przed beanami, które go potrzebują (DataSource), i zatrzymuje przy zamknięciu kontekstu.
 * {@code @ServiceConnection} sprawia, że Boot sam bierze z kontenera adres JDBC, użytkownika i hasło.
 * <p>
 * Klasę dołączasz przez {@code @Import(PostgresTestcontainersConfig.class)} w testach albo przez
 * {@code SpringApplication.from(...).with(PostgresTestcontainersConfig.class)} w TestTest06Application (sekcja 5).
 * Wszystkie klasy testowe z tą samą konfiguracją dzielą JEDEN kontekst z pamięci podręcznej — więc i jeden kontener.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfig {

    @Bean
    @ServiceConnection // service connection = połączenie z usługą (adres, login, hasło brane z kontenera)
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine"); // ZAWSZE konkretna wersja obrazu, nie "latest"
    }
}
