package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Klasa startowa lekcji Boot03SpringApplication — aplikacja „wsadowa” (batch: uruchom, zrób zadanie, zakończ).
 * Zamiast skrótu {@code SpringApplication.run(Boot03Application.class, args)} tworzy obiekt
 * {@link SpringApplication}, ustawia go i dopiero uruchamia. Na końcu kończy proces z kodem wyjścia policzonym
 * przez beany {@code ExitCodeGenerator}.
 *
 * <p>Spróbuj ▶ z argumentami programu: {@code --limit=3 plik.csv}, potem {@code --limit=0} (kod wyjścia 3 —
 * IntelliJ pokaże „Process finished with exit code 3”), potem {@code --limit=abc} (aplikacja nie wystartuje).</p>
 */
@SpringBootApplication
public class Boot03Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        application.setBannerMode(Banner.Mode.CONSOLE);   // banner = napis „Spring” przy starcie (OFF — wyłącz)
        ConfigurableApplicationContext context = application.run(args);
        // exit: zamyka kontekst i zwraca kod z beanów ExitCodeGenerator (pierwszy niezerowy, wg @Order)
        int exitCode = SpringApplication.exit(context);
        System.exit(exitCode);
    }
}
