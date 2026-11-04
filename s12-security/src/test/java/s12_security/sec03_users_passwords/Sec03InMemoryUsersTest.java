package s12_security.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Sec03UsersPasswords: {@code InMemoryUserDetailsManager} — użytkownicy w pamięci (Sec01Basics, Sec02Authorization).
 * Zwykły test jednostkowy.
 */
class Sec03InMemoryUsersTest {

    @Test
    @DisplayName("Sekcja 1: InMemoryUserDetailsManager zwraca KOPIĘ użytkownika, a dane znikają po restarcie (nauka i testy, nie produkcja)")
    void inMemoryManager() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(
                User.withUsername("anna").password(encoder.encode("haslo-testowe")).roles("CUSTOMER").build());
        manager.createUser(User.withUsername("ewa").password(encoder.encode("haslo-testowe")).roles("EMPLOYEE").build());

        UserDetails anna = manager.loadUserByUsername("anna");
        assertThat(manager.userExists("ewa")).isTrue();
        assertThat(anna).isNotSameAs(manager.loadUserByUsername("anna"));    // za każdym razem nowy obiekt
        assertThat(encoder.matches("haslo-testowe", anna.getPassword())).isTrue();
    }
}
