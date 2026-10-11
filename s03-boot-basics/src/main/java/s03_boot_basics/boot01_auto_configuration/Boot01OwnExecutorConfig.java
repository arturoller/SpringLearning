package s03_boot_basics.boot01_auto_configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Boot01OwnExecutorConfig = „moja konfiguracja z własnym wykonawcą zadań” (executor = wykonawca, obiekt, który
 * uruchamia zadania). Definiuje bean {@code shopExecutor} typu Executor.
 *
 * <p>Celowo BEZ adnotacji {@code @Configuration}: leży w pakiecie skanowanym przez Boot01Application, więc z adnotacją
 * trafiłaby do kontekstu lekcji i zmieniła jej wynik. Testy rejestrują ją ręcznie
 * ({@code ApplicationContextRunner.withUserConfiguration(...)}) — wtedy metody {@code @Bean} działają także bez
 * {@code @Configuration} (tzw. tryb „lite”, lekki).</p>
 *
 * <p>Efekt: autokonfiguracja TaskExecutionAutoConfiguration widzi, że Executor już jest, i NIE tworzy swojego
 * {@code applicationTaskExecutor} — to jest „wycofanie się” (back-off) autokonfiguracji.</p>
 */
public class Boot01OwnExecutorConfig {

    /** SyncTaskExecutor = wykonawca synchroniczny: uruchamia zadanie od razu, w tym samym wątku. Prosty i bez wątków. */
    @Bean
    Executor shopExecutor() {
        return new SyncTaskExecutor();
    }
}
