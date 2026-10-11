package s04_web_rest.rest06_cors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CorsConfig = globalna konfiguracja CORS — sekcja 4 lekcji Rest06Cors.
 * {@code WebMvcConfigurer} (konfigurator Spring MVC) — interfejs z metodami domyślnymi; nadpisujesz tylko to, co
 * zmieniasz. {@code addCorsMappings} (dodaj mapowania CORS) — reguły dla wzorców adresów, w jednym miejscu.
 * Leży w pakiecie lekcji, więc skanuje go tylko Rest06Application (inne lekcje go nie widzą).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/orders/**")                  // dla jakich adresów
                .allowedOrigins("https://panel.example.com")    // skąd (pochodzenie strony w przeglądarce)
                .allowedMethods("GET", "POST")                  // jakie metody HTTP
                .allowedHeaders("Content-Type")                 // jakie nagłówki może wysłać przeglądarka
                .exposedHeaders("X-Total-Count")                // jakie nagłówki odpowiedzi JavaScript może przeczytać
                .maxAge(3600);                                  // ile sekund przeglądarka pamięta wynik preflight
    }
}
