package s04_web_rest.rest07_api_versioning;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

/**
 * ApiVersioningConfig = włączenie wersjonowania API (Spring Framework 7) — sekcja 2 lekcji Rest07ApiVersioning.
 * {@code configureApiVersioning} (skonfiguruj wersjonowanie API) — metoda WebMvcConfigurer, nowa w Spring Framework 7.
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        StandardApiVersionDeprecationHandler deprecation = new StandardApiVersionDeprecationHandler();
        deprecation.configureVersion("1.0")                       // wersja 1.0 jest przestarzała (deprecated):
                .setDeprecationDate(ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .setSunsetDate(ZonedDateTime.of(2027, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .setDeprecationLink(URI.create("https://sklep.example.com/docs/api-v2"));

        configurer
                .useRequestHeader("API-Version")                  // wersja z nagłówka zapytania API-Version
                .addSupportedVersions("1.0", "1.1", "2.0")        // obsługiwane wersje (inne → 400)
                .setDeprecationHandler(deprecation);              // nagłówki Deprecation/Sunset/Link dla 1.0
    }
}
