package s04_web_rest.rest06_cors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sekcja 3 lekcji Rest06Cors: GLOBALNA konfiguracja CORS dla API sklepu (wszystkie ścieżki /rest06/api/**).
 * <p>
 * WebMvcConfigurer (konfigurator Spring MVC) to interfejs z metodami „dopisz coś do ustawień MVC”. Boot zbiera
 * wszystkie beany tego typu i woła ich metody przy budowaniu Spring MVC. Ta klasa leży w pakiecie lekcji, więc
 * widzi ją tylko kontekst tej lekcji (Rest06Application) — inne lekcje działu nie dostaną tych ustawień.
 */
@Configuration // configuration = konfiguracja; klasa z ustawieniami, sama też jest beanem
public class CorsConfig implements WebMvcConfigurer {

    /** Adres frontendu (aplikacji w przeglądarce) uruchomionego lokalnie przez programistę. */
    public static final String DEV_FRONTEND = "http://localhost:3000";

    /** Adres „prawdziwego” sklepu internetowego (frontend na produkcji). */
    public static final String SHOP_FRONTEND = "https://sklep.example.com";

    @Override
    public void addCorsMappings(CorsRegistry registry) { // registry = rejestr (spis reguł CORS)
        registry.addMapping("/rest06/api/**")                  // których ścieżek dotyczy reguła
                .allowedOrigins(DEV_FRONTEND, SHOP_FRONTEND)    // origins = pochodzenia, którym ufamy (dokładne adresy)
                .allowedMethods("GET", "POST", "PUT")           // methods = metody HTTP (DELETE celowo NIE)
                .allowedHeaders("Content-Type", "X-Shop-Client") // headers = nagłówki, które klient może wysłać
                .exposedHeaders("X-Total-Count")                // exposed = ujawnione: nagłówki odpowiedzi widoczne dla JS
                .allowCredentials(true)                         // credentials = poświadczenia: ciasteczka, logowanie
                .maxAge(3600);                                  // max age = jak długo (s) przeglądarka pamięta preflight
    }
}
