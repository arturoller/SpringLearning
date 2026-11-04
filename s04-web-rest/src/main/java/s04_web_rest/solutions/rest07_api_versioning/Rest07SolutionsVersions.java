package s04_web_rest.solutions.rest07_api_versioning;

import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;

import java.net.URI;
import java.time.ZonedDateTime;

/** Rozwiązania wzorcowe ćwiczeń 4–5 lekcji Rest07ApiVersioning. */
public final class Rest07SolutionsVersions {

    private static final SemanticApiVersionParser PARSER = new SemanticApiVersionParser();

    private Rest07SolutionsVersions() {
    }

    /** Rozwiązanie 4: parser zamienia napis na wersję major.minor.patch, a wersje porównuje się liczbowo. */
    public static boolean isNewer(String candidate, String current) {
        return PARSER.parseVersion(candidate).compareTo(PARSER.parseVersion(current)) > 0;
    }

    /** Rozwiązanie 5: configureVersion(...) zwraca obiekt, na którym ustawiamy daty i link. */
    public static StandardApiVersionDeprecationHandler deprecationOf1dot1() {
        StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
        handler.configureVersion("1.1")
                .setDeprecationDate(ZonedDateTime.parse("2026-06-01T00:00:00Z"))
                .setSunsetDate(ZonedDateTime.parse("2027-06-30T00:00:00Z"))
                .setSunsetLink(URI.create("https://sklep.example.com/docs/api/v2"));
        return handler;
    }
}
