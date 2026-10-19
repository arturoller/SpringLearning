package s22_extras.ext03_oauth2_keycloak;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Konfiguracja Resource Servera (serwera zasobów) lekcji Ext03OAuth2Keycloak. Dekoder tokenów (JwtDecoder) tworzy
 * Boot z właściwości spring.security.oauth2.resourceserver.jwt.issuer-uri — tu tylko reguły dostępu i mapowanie ról.
 */
@Configuration
public class Ext03SecurityConfig {

    /**
     * Zamiana tokenu na Authentication: uprawnienia = SCOPE_... (domyślny konwerter, z claimu „scope”)
     * + ROLE_... (role Keycloaka z realm_access.roles). Nazwa użytkownika z claimu preferred_username
     * (domyślnie byłby „sub” — w Keycloaku to UUID, nieczytelny w logach).
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new DelegatingJwtGrantedAuthoritiesConverter(
                new JwtGrantedAuthoritiesConverter(),     // scope "profile email" → SCOPE_profile, SCOPE_email
                new KeycloakRealmRoleConverter()));       // realm_access.roles ["klient"] → ROLE_KLIENT
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    @Bean
    SecurityFilterChain ext03Chain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("PRACOWNIK")   // = uprawnienie ROLE_PRACOWNIK
                        .requestMatchers("/api/orders/**").hasRole("KLIENT")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))   // bez sesji
                .csrf(csrf -> csrf.disable())   // token w nagłówku Authorization, nie w ciasteczku — CSRF nie grozi
                .build();
    }
}
