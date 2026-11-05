package s11_testing.test06_testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * JEDEN kontener PostgreSQL dla RÓŻNYCH konfiguracji testów (sekcja 4 lekcji Test06Testcontainers). Pole jest
 * statyczne (w interfejsie pola są zawsze {@code public static final}), więc istnieje jeden obiekt kontenera na cały
 * przebieg testów (na całą maszynę JVM). Klasy testowe dołączają go adnotacją
 * {@code @ImportTestcontainers(SharedPostgres.class)}: Boot rejestruje pole jako bean, uruchamia kontener (jeśli
 * jeszcze nie działa) i tworzy z niego połączenie ({@code @ServiceConnection}).
 * <p>
 * Kto go zatrzyma? Ryuk — kontener-sprzątacz Testcontainers — usuwa go po zakończeniu JVM.
 */
public interface SharedPostgres {

    @ServiceConnection
    PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
}
