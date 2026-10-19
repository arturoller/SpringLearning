package s12_security.sec01_basics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.util.Map;

/**
 * Klasa startowa „SAM STARTER” — aplikacja dokładnie w takim stanie, jak zaraz po dodaniu
 * {@code spring-boot-starter-security}: jest kontroler, nie ma żadnej własnej konfiguracji bezpieczeństwa.
 *
 * <p>Celowo BEZ {@code @SpringBootApplication}: ta adnotacja włącza skanowanie pakietu ({@code @ComponentScan}),
 * a w tym pakiecie leży Sec01SecurityConfig z własnymi regułami — wtedy zobaczylibyśmy NASZE reguły, a nie domyślne.
 * Zamiast tego: {@code @EnableAutoConfiguration} (włącz autokonfigurację) + {@code @Import(ShopController.class)}
 * (dołącz tylko kontroler). Uruchom ▶ i poszukaj w logu linii „Using generated security password”.</p>
 *
 * <p>Ustawienie {@code spring.security.user.name=user} — wyjaśnienie w Sec01Basics, sekcja 1 (PUŁAPKA z drugim
 * starterem). W świeżym projekcie z samym starterem security ta linia NIE jest potrzebna.</p>
 */
@EnableAutoConfiguration   // enable auto configuration = włącz autokonfigurację
@Import(ShopController.class)
public class Sec01StarterOnlyApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Sec01StarterOnlyApplication.class);
        app.setDefaultProperties(Map.of("spring.security.user.name", "user"));   // default properties = ustawienia domyślne
        app.run(args);
    }
}
