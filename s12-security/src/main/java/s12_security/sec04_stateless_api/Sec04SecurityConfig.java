package s12_security.sec04_stateless_api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Sec04SecurityConfig = DWA łańcuchy filtrów w jednej aplikacji (sekcja 1 lekcji Sec04StatelessApi):
 * <ol>
 *   <li>{@code apiChain} — dla {@code /api/**}: API bez sesji, bez CSRF, z CORS i błędami w JSON,</li>
 *   <li>{@code webChain} — dla całej reszty: klasyczna aplikacja dla przeglądarki z formularzem logowania, sesją
 *       i ochroną CSRF.</li>
 * </ol>
 * FilterChainProxy wybiera PIERWSZY pasujący łańcuch, dlatego {@code @Order} (kolejność): węższy ({@code /api/**})
 * musi być przed łańcuchem „na wszystko”.
 */
@Configuration
public class Sec04SecurityConfig {

    @Bean
    @Order(1)   // order = kolejność: ten łańcuch sprawdzany jako pierwszy
    SecurityFilterChain apiChain(HttpSecurity http) {
        ProblemDetailSecurityHandler problems = new ProblemDetailSecurityHandler();
        http.securityMatcher("/api/**");   // security matcher = „ten łańcuch dotyczy tylko tych adresów”
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
        http.httpBasic(basic -> basic.authenticationEntryPoint(problems));   // złe hasło → 401 w JSON
        http.exceptionHandling(ex -> ex                                       // exception handling = obsługa odmów
                .authenticationEntryPoint(problems)                           // niezalogowany → 401 w JSON
                .accessDeniedHandler(problems));                              // brak uprawnień → 403 w JSON
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));     // stateless = bezstanowy: bez sesji
        http.csrf(AbstractHttpConfigurer::disable);   // wolno: brak sesji i ciasteczek logowania (sekcja 3)
        http.cors(Customizer.withDefaults());         // CORS wg beana corsConfigurationSource (sekcja 4)
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain webChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated());
        http.formLogin(Customizer.withDefaults());   // sesja (JSESSIONID) i CSRF — domyślnie WŁĄCZONE
        return http.build();
    }

    /**
     * CORS (Cross-Origin Resource Sharing — współdzielenie zasobów między pochodzeniami): przeglądarka pozwoli
     * frontendowi z http://localhost:5173 wołać nasze /api/**. Nazwa beana „corsConfigurationSource” jest ważna —
     * po niej szuka go {@code http.cors(withDefaults())}.
     */
    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));          // allowed origins = dozwolone pochodzenia
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);   // max age = jak długo (s) przeglądarka może pamiętać odpowiedź na preflight
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** Hasła jako skróty {bcrypt} (Sec03UsersPasswords); wartości TESTOWE. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password(encoder.encode("anna-test-123")).roles("ADMIN").build(),
                User.withUsername("piotr").password(encoder.encode("piotr-test-123")).roles("USER").build());
    }
}
