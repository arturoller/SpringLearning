package s04_web_rest.solutions.rest06_cors;

import org.springframework.web.cors.CorsConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń 2–4 lekcji Rest06Cors — zajrzyj dopiero po własnej próbie! */
public final class Rest06Solutions {

    private static final Set<String> SIMPLE_METHODS = Set.of("GET", "HEAD", "POST");
    private static final Set<String> SAFELISTED_HEADERS = Set.of("accept", "accept-language", "content-language",
            "content-type");
    private static final Set<String> SIMPLE_CONTENT_TYPES = Set.of("application/x-www-form-urlencoded",
            "multipart/form-data", "text/plain");

    private Rest06Solutions() {
    }

    /** Rozwiązanie 2: to samo, co zrobiłoby addCorsMappings(...) albo @CrossOrigin z tymi atrybutami. */
    public static CorsConfiguration shopConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://sklep.example"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(1800L);
        return config;
    }

    /** Rozwiązanie 3: wzorzec pochodzenia — Spring odeśle w odpowiedzi KONKRETNE pochodzenie, a nie "*". */
    public static CorsConfiguration subdomainsWithCredentials() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("https://*.sklep.example"));
        config.setAllowedMethods(List.of("GET"));
        config.setAllowCredentials(true);
        return config;
    }

    /** Rozwiązanie 4: zapytanie proste = prosta metoda + tylko dozwolone nagłówki + prosty Content-Type. */
    public static boolean needsPreflight(String method, Map<String, String> headers) {
        if (!SIMPLE_METHODS.contains(method.toUpperCase(Locale.ROOT))) {
            return true;
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            String name = header.getKey().toLowerCase(Locale.ROOT);
            if (!SAFELISTED_HEADERS.contains(name)) {
                return true;
            }
            if (name.equals("content-type")) {
                String type = header.getValue().split(";")[0].strip().toLowerCase(Locale.ROOT);
                if (!SIMPLE_CONTENT_TYPES.contains(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
