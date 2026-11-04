package s12_security.solutions.sec04_stateless_api;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Sec04StatelessApi. */
public final class ExerciseCors {

    private ExerciseCors() {
    }

    public static UrlBasedCorsConfigurationSource source() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/sec04/cw/**", config);
        return source;
    }
}
