package s04_web_rest.solutions.rest06_cors;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rest06Cors — zajrzyj dopiero po własnej próbie! */
public final class Rest06SolutionsCors {

    private Rest06SolutionsCors() {
    }

    /** Rozwiązanie 1: URI rozbiera adres na części; port -1 oznacza „nie podano jawnie”. */
    public static String originOf(String url) {
        URI uri = URI.create(url);
        String origin = uri.getScheme() + "://" + uri.getHost();
        return uri.getPort() == -1 ? origin : origin + ":" + uri.getPort();
    }

    /** Rozwiązanie 2: to samo pochodzenie = identyczny schemat, host i port (ścieżka nie ma znaczenia). */
    public static boolean sameOrigin(String firstUrl, String secondUrl) {
        return originOf(firstUrl).equals(originOf(secondUrl));
    }

    /** Rozwiązanie 3: dokładny origin, dwie metody, jeden nagłówek, maxAge w sekundach. */
    public static CorsConfiguration shopFrontConfig() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedHeader("Content-Type");
        config.setMaxAge(1800L);
        return config;
    }

    /** Rozwiązanie 4: wzorzec z gwiazdką → allowedOriginPatterns (allowedOrigins z "*" + poświadczenia to błąd). */
    public static CorsConfiguration partnersConfig() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("https://*.partner.example.com");
        config.addAllowedMethod("GET");
        config.setAllowCredentials(true);
        config.addExposedHeader("X-Total-Count");
        return config;
    }

    /** Rozwiązanie 5: źródło konfiguracji „ścieżka → reguła”, takie samo, jakie buduje CorsRegistry w WebMvcConfigurer. */
    public static CorsConfigurationSource source() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/rest06/cw/**", shopFrontConfig());
        return source;
    }
}
