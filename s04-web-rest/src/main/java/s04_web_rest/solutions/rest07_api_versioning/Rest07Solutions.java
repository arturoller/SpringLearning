package s04_web_rest.solutions.rest07_api_versioning;

import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/** Rozwiązania wzorcowe ćwiczeń 3–4 lekcji Rest07ApiVersioning — zajrzyj dopiero po własnej próbie! */
public final class Rest07Solutions {

    private static final SemanticApiVersionParser PARSER = new SemanticApiVersionParser();

    private Rest07Solutions() {
    }

    /** Rozwiązanie 3: ten sam parser, którego Spring używa domyślnie — wersje porównywane liczbowo. */
    public static int compareVersions(String a, String b) {
        return Integer.signum(PARSER.parseVersion(a).compareTo(PARSER.parseVersion(b)));
    }

    /** Rozwiązanie 4: jak w ApiDeprecationConfig, tylko dla wersji 1.1 i z innymi datami. */
    public static ApiVersionDeprecationHandler sunsetPlan() {
        StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
        handler.configureVersion("1.1")
                .setDeprecationDate(ZonedDateTime.of(2026, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .setDeprecationLink(URI.create("https://sklep.example/api/zmiany-1-1"))
                .setSunsetDate(ZonedDateTime.of(2027, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC));
        return handler;
    }
}
