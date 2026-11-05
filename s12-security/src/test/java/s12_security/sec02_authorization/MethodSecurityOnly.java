package s12_security.sec02_authorization;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Pomocnik testów ćwiczenia 3 (i wzorca): włącza samo bezpieczeństwo metod, BEZ hierarchii ról i bez weba.
 * Celowo bez {@code @Configuration}: klasy testowe leżą w tym samym pakiecie co {@code Sec02Application}, więc
 * {@code @SpringBootTest} lekcji zeskanowałby taką konfigurację. Test podaje ją jawnie w {@code @SpringJUnitConfig}.
 */
@EnableMethodSecurity
public class MethodSecurityOnly {
}
