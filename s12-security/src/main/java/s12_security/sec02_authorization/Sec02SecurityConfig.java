package s12_security.sec02_authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Konfiguracja bezpieczeństwa lekcji Sec02Authorization: reguły adresów (sekcja 1), role i uprawnienia (sekcja 2),
 * bezpieczeństwo metod (sekcja 3, {@code @EnableMethodSecurity}) i hierarchia ról (sekcja 4).
 */
@Configuration
@EnableMethodSecurity   // enable method security = włącz bezpieczeństwo metod (@PreAuthorize, @PostAuthorize, @PostFilter…)
public class Sec02SecurityConfig {

    @Bean
    SecurityFilterChain sec02FilterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        // Reguły czytane OD GÓRY — pierwsza pasująca wygrywa. Dlatego najpierw szczegółowe, potem ogólne.
                        .requestMatchers(HttpMethod.GET, "/sec02/products/**").permitAll()      // katalog: każdy
                        .requestMatchers(HttpMethod.POST, "/sec02/products/**").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/sec02/products/**").hasRole("ADMIN")
                        .requestMatchers("/sec02/orders/**").hasAnyRole("CUSTOMER", "EMPLOYEE") // has any role = ma którąkolwiek
                        .requestMatchers("/sec02/reports/**").hasAuthority("REPORTS_READ")      // uprawnienie, nie rola
                        .requestMatchers("/sec02/admin/**").hasRole("ADMIN")
                        .requestMatchers("/error").permitAll()                                   // Sec01Basics, sekcja 5
                        .anyRequest().denyAll())       // deny all = odmów wszystkim: adres bez reguły jest ZAMKNIĘTY (sekcja 5)
                .httpBasic(Customizer.withDefaults())
                // CSRF wyłączony TYLKO dlatego, że w tej lekcji API wołają programy (testy, plik .http), a nie przeglądarka.
                // Przeglądarka sama dołącza zapamiętane dane Basic i ciasteczka — wtedy CSRF MUSI zostać. Kiedy wolno go wyłączyć:
                // Sec04StatelessApi.
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Hierarchia ról: ADMIN ma wszystko, co EMPLOYEE, a EMPLOYEE wszystko, co CUSTOMER. Spring Security 7 sam znajduje ten bean
     * i używa go i w regułach adresów, i w {@code @PreAuthorize} (testy Sec02UrlRulesTest.roleHierarchyForUrls
     * i Sec02MethodSecurityTest.roleHierarchyForMethods). Metoda {@code static}, bo bean jest potrzebny bardzo wcześnie —
     * przy budowie infrastruktury bezpieczeństwa metod.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()     // default role prefix = domyślny prefiks ROLE_
                .role("ADMIN").implies("EMPLOYEE")           // implies = zawiera w sobie
                .role("EMPLOYEE").implies("CUSTOMER")
                .build();
    }

    @Bean
    UserDetailsService sec02Users() {
        // Hasła TYLKO DO NAUKI, zapisane jawnie ({noop}) — porządne hashowanie: Sec03UsersPasswords.
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build(),
                User.withUsername("piotr").password("{noop}haslo-testowe").roles("CUSTOMER").build(),
                // authorities(...) USTAWIA całą listę naraz, więc rolę podajemy tu jako "ROLE_EMPLOYEE" (z prefiksem!).
                User.withUsername("ewa").password("{noop}haslo-testowe").authorities("ROLE_EMPLOYEE", "REPORTS_READ").build(),
                User.withUsername("admin").password("{noop}haslo-testowe").roles("ADMIN").build());
    }
}
