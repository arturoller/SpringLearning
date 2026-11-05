package s12_security.sec06_security_testing;

import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Pomocnik testów sekcji 3 lekcji Sec06SecurityTesting (i ćwiczenia 4): samo bezpieczeństwo metod + hierarchia ról z aplikacji,
 * bez weba i bez łańcuchów filtrów. Celowo bez {@code @Configuration}: klasy testowe leżą w pakiecie {@code Sec06Application},
 * więc {@code @SpringBootTest} lekcji zeskanowałby taką konfigurację. Testy podają ją jawnie w {@code @SpringJUnitConfig}.
 */
@EnableMethodSecurity
public class MethodSecurityTestConfig {

    @Bean
    static RoleHierarchy roleHierarchy() {
        return Sec06SecurityConfig.roleHierarchy();
    }
}
