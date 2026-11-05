package s04_web_rest.rest07_api_versioning;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.time.ZonedDateTime;

/**
 * Sekcja 4 lekcji Rest07ApiVersioning: wersja 1.0 jest WYCOFYWANA (deprecated). Każda odpowiedź w wersji 1.0
 * dostaje nagłówki Deprecation, Sunset i Link — klient widzi, że czas przejść na 2.0.
 * <p>
 * Tego nie da się ustawić właściwością, więc używamy WebMvcConfigurer.configureApiVersioning (configure = skonfiguruj;
 * versioning = wersjonowanie). Boot najpierw stosuje właściwości spring.mvc.apiversion.*, a potem woła tę metodę
 * na TYM SAMYM obiekcie ApiVersionConfigurer — ustawienia się sumują.
 * Klasa leży w pakiecie lekcji, więc działa tylko w kontekście tej lekcji.
 */
@Configuration
public class ApiDeprecationConfig implements WebMvcConfigurer {

    /** Daty stałe (nie „teraz”), żeby nagłówki w testach były zawsze takie same. */
    static final ZonedDateTime DEPRECATED_SINCE = ZonedDateTime.parse("2026-01-01T00:00:00Z");
    static final ZonedDateTime SUNSET = ZonedDateTime.parse("2027-01-01T00:00:00Z");
    static final URI MIGRATION_GUIDE = URI.create("https://sklep.example.com/docs/api/migracja-v2");

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
        handler.configureVersion("1.0")
                .setDeprecationDate(DEPRECATED_SINCE)  // deprecation = wycofanie: „od kiedy odradzamy”
                .setSunsetDate(SUNSET)                 // sunset = zachód słońca: „od kiedy przestanie działać”
                .setSunsetLink(MIGRATION_GUIDE);       // link = gdzie przeczytać, co zamiast
        configurer.setDeprecationHandler(handler);
    }
}
