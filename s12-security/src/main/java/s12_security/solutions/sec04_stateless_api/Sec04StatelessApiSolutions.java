package s12_security.solutions.sec04_stateless_api;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń 1–3 lekcji Sec04StatelessApi (ćwiczenie 4: SolutionJsonEntryPoint). */
public final class Sec04StatelessApiSolutions {

    /** Metody „bezpieczne” — nie zmieniają stanu, więc CsrfFilter ich nie sprawdza. */
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");

    private Sec04StatelessApiSolutions() {
    }

    static boolean solution1(String httpMethod) {
        return !SAFE_METHODS.contains(httpMethod.toUpperCase(Locale.ROOT));
    }

    static CorsConfigurationSource solution2() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://sklep.example"));
        config.setAllowedMethods(List.of("GET", "POST"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(1800L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    static SecurityFilterChain solution3(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                .anyRequest().authenticated());
        http.httpBasic(Customizer.withDefaults());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
