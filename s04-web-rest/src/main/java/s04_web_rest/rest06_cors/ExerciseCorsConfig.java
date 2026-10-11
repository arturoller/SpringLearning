package s04_web_rest.rest06_cors;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ExerciseCorsConfig = konfiguracja CORS — SZKIELET ćwiczenia 3 lekcji Rest06Cors.
 * Celowo BEZ {@code @Configuration}: aplikacja lekcji jej nie skanuje (nierozwiązany szkielet nie psuje
 * Rest06Application). Test ćwiczenia dokłada ją sam przez {@code @Import}.
 */
public class ExerciseCorsConfig implements WebMvcConfigurer {

    /**
     * ĆWICZENIE 3 (trudniejsze): dla adresów /api/exercise/** pozwól stronie https://raporty.example.com używać metod
     * GET i PUT, wysyłać nagłówek Content-Type, a wynik preflight niech przeglądarka pamięta 600 sekund.
     * Wszystko inne (inne pochodzenie, DELETE) ma dostać 403. Podpowiedź: jak CorsConfig — registry.addMapping(...)
     * .allowedOrigins(...).allowedMethods(...).allowedHeaders(...).maxAge(...).
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
