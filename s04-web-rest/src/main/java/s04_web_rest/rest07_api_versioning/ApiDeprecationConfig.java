package s04_web_rest.rest07_api_versioning;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * ApiDeprecationConfig = plan wycofania wersji 1.0 (deprecation = oznaczenie jako przestarzałe; sunset = „zachód
 * słońca”, data wyłączenia). Resztę wersjonowania ustawiają właściwości spring.mvc.apiversion.* (profil rest07);
 * tu — w kodzie — dokładamy to, czego właściwościami ustawić się nie da.
 *
 * <p>{@code configureApiVersioning(ApiVersionConfigurer)} — metoda WebMvcConfigurer z Spring Framework 7. Boot woła
 * swoją wersję (z właściwościami) i naszą na TYM SAMYM obiekcie konfiguratora, więc ustawienia się sumują.
 * Daty są STAŁE (a nie „za pół roku od dziś”) — odpowiedź ma być taka sama przy każdym uruchomieniu.</p>
 */
@Configuration
public class ApiDeprecationConfig implements WebMvcConfigurer {

    static final ZonedDateTime DEPRECATED_SINCE = ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    static final ZonedDateTime SUNSET = ZonedDateTime.of(2027, 3, 31, 0, 0, 0, 0, ZoneOffset.UTC);

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
        handler.configureVersion("1.0")
                .setDeprecationDate(DEPRECATED_SINCE)                                 // nagłówek Deprecation
                .setDeprecationLink(URI.create("https://sklep.example/api/migracja-v2"))   // nagłówek Link
                .setSunsetDate(SUNSET);                                               // nagłówek Sunset
        configurer.setDeprecationHandler(handler);
    }
}
