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
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

/**
 * Konfiguracja lekcji Sec04StatelessApi: DWA łańcuchy filtrów w jednej aplikacji.
 * <ol>
 *   <li>{@link #apiChain} ({@code @Order(1)}, tylko {@code /sec04/api/**}) — API bez sesji: STATELESS, CSRF wyłączony, CORS,
 *       błędy 401/403 jako ProblemDetail.</li>
 *   <li>{@link #browserChain} ({@code @Order(2)}, wszystko inne) — strona dla przeglądarki: sesja i CSRF WŁĄCZONY (domyślnie).</li>
 * </ol>
 * {@code FilterChainProxy} bierze PIERWSZY łańcuch (według {@code @Order}), którego {@code securityMatcher} pasuje do adresu.
 */
@Configuration
public class Sec04SecurityConfig {

    /** Adres frontendu (SPA) — jedyne obce pochodzenie, któremu API pozwala na zapytania z przeglądarki (sekcja 3). */
    public static final String FRONTEND_ORIGIN = "http://localhost:3000";

    @Bean
    @Order(1)   // order = kolejność: ten łańcuch jest sprawdzany jako pierwszy
    SecurityFilterChain apiChain(HttpSecurity http, ProblemDetailSecurityHandlers problemHandlers) {
        http
                .securityMatcher("/sec04/api/**")       // security matcher = dla jakich adresów jest TEN łańcuch
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/sec04/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/sec04/api/products/**").hasRole("EMPLOYEE")
                        .requestMatchers("/sec04/api/orders/**").authenticated()
                        .requestMatchers("/sec04/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                // Sekcja 1: STATELESS = Spring Security NIE tworzy sesji i NIE czyta kontekstu z sesji.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Sekcja 2: CSRF wyłączony, bo API nie używa ciasteczek — przeglądarka nie dołączy niczego "sama".
                .csrf(AbstractHttpConfigurer::disable)
                // Sekcja 3: filtr CORS PRZED uwierzytelnianiem — używa beana o nazwie corsConfigurationSource.
                .cors(Customizer.withDefaults())
                // Sekcja 4: złe hasło (filtr Basic) → ten sam ProblemDetail co brak logowania.
                .httpBasic(basic -> basic.authenticationEntryPoint(problemHandlers))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(problemHandlers)   // brak logowania → 401 + JSON
                        .accessDeniedHandler(problemHandlers));      // brak uprawnień → 403 + JSON
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain browserChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sec04/web/**").authenticated()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                // Brak .csrf(...) i .sessionManagement(...) = ustawienia domyślne: sesja, gdy potrzebna, i CSRF WŁĄCZONY.
                .formLogin(Customizer.withDefaults())     // formularz /login: logujesz się RAZ, potem pamięta Cię sesja
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Reguły CORS dla API (sekcja 3). Nazwa beana {@code corsConfigurationSource} jest ważna — tak szuka go
     * {@code http.cors(Customizer.withDefaults())}.
     */
    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration api = new CorsConfiguration();
        api.setAllowedOrigins(List.of(FRONTEND_ORIGIN));
        api.setAllowedMethods(List.of("GET", "POST"));
        api.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        api.setMaxAge(Duration.ofHours(1));       // max age = przeglądarka pamięta odpowiedź na preflight przez godzinę
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/sec04/api/**", api);
        return source;
    }

    @Bean
    ProblemDetailSecurityHandlers problemHandlers(JsonMapper jsonMapper) {
        return new ProblemDetailSecurityHandlers(jsonMapper);
    }

    @Bean
    UserDetailsService sec04Users() {
        // Hasła TYLKO DO NAUKI, zapisane jawnie ({noop}) — porządne hashowanie: Sec03UsersPasswords.
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build(),
                User.withUsername("ewa").password("{noop}haslo-testowe").roles("EMPLOYEE").build(),
                User.withUsername("admin").password("{noop}haslo-testowe").roles("ADMIN").build());
    }
}
