package s11_testing.test06_testcontainers;

import org.springframework.boot.SpringApplication;

/**
 * Lekcja Test06Testcontainers, sekcja 5: uruchomienie APLIKACJI lokalnie z prawdziwym PostgreSQL w kontenerze.
 * Leży w src/test/java, bo korzysta z zależności testowych (Testcontainers) — do produkcyjnego jara nie trafia.
 * (Spring Initializr generuje taką klasę pod nazwą „Test” + nazwa aplikacji, np. TestShopApplication.)
 *
 * <p>{@code SpringApplication.from(Test06Application::main)} uruchamia ZWYKŁĄ metodę main aplikacji,
 * a {@code .with(Test06Containers.class)} dokłada konfigurację z beanem-kontenerem: Spring startuje PostgreSQL,
 * a {@code @ServiceConnection} podłącza do niego DataSource. Uruchom ▶ przy main (Docker musi działać) i wyślij
 * zapytania z s11-testing/http/test06_testcontainers.http. Zatrzymanie aplikacji zatrzymuje kontener.</p>
 *
 * <p>PUŁAPKA: (sprawdzona uruchomieniem) {@code from(...)} USTAWIA dodatkowe profile aplikacji od nowa — profil
 * dodany w {@code Test06Application.main} przez {@code setAdditionalProfiles("test06")} ginie (w logu: „No active
 * profile set”). Dlatego profil podajemy tutaj: {@code withAdditionalProfiles("test06")}.</p>
 */
public class Test06DevApplication {

    public static void main(String[] args) {
        SpringApplication.from(Test06Application::main)
                .with(Test06Containers.class)
                .withAdditionalProfiles("test06")
                .run(args);
    }
}
