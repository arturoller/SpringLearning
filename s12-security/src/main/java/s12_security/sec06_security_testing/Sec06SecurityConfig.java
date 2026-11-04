package s12_security.sec06_security_testing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;

/**
 * Konfiguracja aplikacji testowanej w lekcji Sec06SecurityTesting — trzy łańcuchy (powtórka Sec04StatelessApi i Sec05Jwt):
 * <ol>
 *   <li>{@link #tokenChain} — {@code POST /sec06/token} z HTTP Basic (hasło → token).</li>
 *   <li>{@link #panelChain} — {@code /sec06/panel/**}: panel w przeglądarce, sesja, CSRF włączony, Basic.</li>
 *   <li>{@link #apiChain} — reszta: API z tokenem JWT, bez sesji i bez CSRF.</li>
 * </ol>
 * Do tego bezpieczeństwo metod ({@link OrderService}) i hierarchia ról ADMIN > EMPLOYEE > CUSTOMER.
 */
@Configuration
@EnableMethodSecurity
public class Sec06SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain tokenChain(HttpSecurity http) {
        http.securityMatcher("/sec06/token")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain panelChain(HttpSecurity http) {
        http.securityMatcher("/sec06/panel/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("EMPLOYEE"))
                .httpBasic(Customizer.withDefaults());          // sesja i CSRF — ustawienia domyślne
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationConverter rolesConverter) {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/sec06/products").permitAll()
                        .requestMatchers(HttpMethod.POST, "/sec06/products").hasRole("EMPLOYEE")
                        .requestMatchers("/sec06/orders/**").authenticated()      // kto czyje — decyduje OrderService
                        .requestMatchers("/sec06/admin/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("EMPLOYEE")
                .role("EMPLOYEE").implies("CUSTOMER")
                .build();
    }

    /** Klucz HMAC TYLKO DO NAUKI — losowany przy starcie (Sec05Jwt, sekcja 2). */
    @Bean
    SecretKey jwtSecretKey() throws NoSuchAlgorithmException {
        return KeyGenerator.getInstance("HmacSHA256").generateKey();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator();
        timestamps.setClock(clock);
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestamps));
        return decoder;
    }

    /** Twierdzenie {@code roles} → {@code ROLE_…} (Sec05Jwt, sekcja 4). Publiczne, bo używają go też testy z {@code jwt()}. */
    public static JwtGrantedAuthoritiesConverter rolesAuthorities() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        return roles;
    }

    @Bean
    JwtAuthenticationConverter rolesConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesAuthorities());
        return converter;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    UserDetailsService sec06Users() {
        // Hasła TYLKO DO NAUKI, zapisane jawnie ({noop}) — porządne hashowanie: Sec03UsersPasswords.
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build(),
                User.withUsername("piotr").password("{noop}haslo-testowe").roles("CUSTOMER").build(),
                User.withUsername("ewa").password("{noop}haslo-testowe").roles("EMPLOYEE").build(),
                User.withUsername("admin").password("{noop}haslo-testowe").roles("ADMIN").build());
    }
}
