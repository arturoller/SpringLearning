package s12_security.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 1 lekcji Sec03UsersPasswords: {@link JpaUserDetailsService} na prawdziwej bazie H2 z migracjami Flyway.
 * {@code @DataJpaTest} (wycinek JPA) ładuje encje, repozytoria i Flyway — ale nie zwykłe {@code @Service}, więc serwis
 * dołączamy przez {@code @Import}. Profil {@code sec03} włącza Flyway z migracjami tej lekcji.
 */
@DataJpaTest
@ActiveProfiles("sec03")
@Import(JpaUserDetailsService.class)
class Sec03UserDetailsServiceTest {

    @Autowired
    JpaUserDetailsService service;

    @Test
    @DisplayName("Sekcja 1: użytkownik z bazy → UserDetails z hashem, rolą ROLE_CUSTOMER i flagą enabled")
    void loadsUserFromDatabase() {
        UserDetails anna = service.loadUserByUsername("anna");

        assertThat(anna.getUsername()).isEqualTo("anna");
        assertThat(anna.getPassword()).startsWith("{bcrypt}$2a$10$");
        assertThat(anna.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CUSTOMER");
        assertThat(anna.isEnabled()).isTrue();
        assertThat(service.loadUserByUsername("jan").isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Sekcja 1: nieznana nazwa → UsernameNotFoundException (umowa interfejsu UserDetailsService)")
    void unknownUser() {
        assertThatThrownBy(() -> service.loadUserByUsername("nikt")).isInstanceOf(UsernameNotFoundException.class);
    }
}
