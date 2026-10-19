package s12_security.sec06_security_testing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

/**
 * Sec06SecurityConfig = konfiguracja, którą TESTUJEMY w lekcji Sec06SecurityTesting. Zbiera wszystko z działu:
 * <ul>
 *   <li>{@code partnerChain} (Order 1) — {@code /api/partner/**}: API dla partnerów z tokenem JWT (Resource Server),</li>
 *   <li>{@code shopChain} (Order 2) — reszta: HTTP Basic + formularz, sesja i CSRF (domyślnie włączone), reguły ról,
 *       domyślnie „zabronione”,</li>
 *   <li>{@code @EnableMethodSecurity} — {@code @PreAuthorize} w OrderService.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class Sec06SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain partnerChain(HttpSecurity http) {
        http.securityMatcher("/api/partner/**");
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/partner/stock").hasAuthority("SCOPE_stock:read")
                .anyRequest().denyAll());
        http.oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain shopChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                .requestMatchers("/api/orders/**", "/api/me").authenticated()
                .anyRequest().denyAll());
        http.httpBasic(Customizer.withDefaults());
        http.formLogin(Customizer.withDefaults());   // aplikacja obsługuje też przeglądarkę — dlatego CSRF zostaje WŁĄCZONE
        return http.build();
    }

    /**
     * Dekoder tokenów partnerów. W prawdziwym systemie: spring.security.oauth2.resourceserver.jwt.issuer-uri serwera
     * tożsamości. Tu klucz publiczny z pary wygenerowanej przy starcie (TESTOWEJ) — testy i tak nie potrzebują
     * prawdziwych tokenów, bo {@code jwt()} ze spring-security-test wkłada gotowe uwierzytelnienie.
     */
    @Bean
    JwtDecoder jwtDecoder() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) generator.generateKeyPair().getPublic()).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Konta (hasła TESTOWE): anna — ADMIN, piotr — USER. */
    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password(encoder.encode("anna-test-123")).roles("ADMIN").build(),
                User.withUsername("piotr").password(encoder.encode("piotr-test-123")).roles("USER").build());
    }
}
