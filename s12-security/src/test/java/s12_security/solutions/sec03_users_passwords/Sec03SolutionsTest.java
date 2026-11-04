package s12_security.solutions.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import s12_security.sec03_users_passwords.AppUser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy rozwiązań wzorcowych lekcji Sec03UsersPasswords. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. Same testy jednostkowe,
 * bez Springa. Hasła TYLKO DO NAUKI.
 */
@Tag("wzorzec")
class Sec03SolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: za krótkie hasło i hasło z loginem są odrzucane, dobre hasło przechodzi")
    void exercise1() {
        assertThat(PasswordPolicy.problems("anna", "kawa")).containsExactly("ZA_KROTKIE");
        assertThat(PasswordPolicy.problems("anna", "ANNA-lubi-kawe")).containsExactly("ZAWIERA_LOGIN");
        assertThat(PasswordPolicy.problems("anna", "anna1")).containsExactly("ZA_KROTKIE", "ZAWIERA_LOGIN");
        assertThat(PasswordPolicy.problems("anna", "zielona-herbata-o-poranku")).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: limit 72 BAJTÓW — 40 polskich liter to 80 bajtów (za długie), 72 litery a — w sam raz")
    void exercise2() {
        assertThat(PasswordPolicy.problems("anna", "ą".repeat(40))).containsExactly("ZA_DLUGIE");
        assertThat(PasswordPolicy.problems("anna", "x".repeat(72))).isEmpty();
        assertThat(PasswordPolicy.problems("anna", "x".repeat(73))).containsExactly("ZA_DLUGIE");
    }

    @Test
    @DisplayName("Wzorzec 3: nowe hasła {bcrypt} z kosztem 12, stare {noop} działają i są do przepisania")
    void exercise3() {
        PasswordEncoder encoder = ExerciseEncoders.create();
        String hash = encoder.encode("haslo-testowe");
        assertThat(hash).startsWith("{bcrypt}$2a$12$");
        assertThat(encoder.matches("haslo-testowe", hash)).isTrue();
        assertThat(encoder.matches("haslo-testowe", "{noop}haslo-testowe")).isTrue();
        assertThat(encoder.upgradeEncoding("{noop}haslo-testowe")).isTrue();
        assertThat(encoder.upgradeEncoding(hash)).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 4: konto → UserDetails (role ROLE_…, enabled), brak konta → UsernameNotFoundException")
    void exercise4Mapping() {
        ExerciseUserDetailsService service = new ExerciseUserDetailsService(Map.of(
                "ewa", new AppUser("ewa", "{noop}haslo-testowe", "CUSTOMER,EMPLOYEE", true)));
        UserDetails ewa = service.loadUserByUsername("ewa");
        assertThat(ewa.getUsername()).isEqualTo("ewa");
        assertThat(ewa.getPassword()).isEqualTo("{noop}haslo-testowe");
        assertThat(ewa.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_EMPLOYEE");
        assertThat(ewa.isEnabled()).isTrue();
        assertThatThrownBy(() -> service.loadUserByUsername("nikt")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Wzorzec 4: Twój serwis w DaoAuthenticationProvider — dobre hasło loguje, wyłączone konto: DisabledException")
    void exercise4WithProvider() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String hash = encoder.encode("haslo-testowe");
        ExerciseUserDetailsService service = new ExerciseUserDetailsService(Map.of(
                "anna", new AppUser("anna", hash, "CUSTOMER", true),
                "jan", new AppUser("jan", hash, "CUSTOMER", false)));
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(encoder);

        assertThat(provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("anna", "haslo-testowe"))
                .isAuthenticated()).isTrue();
        assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("jan", "haslo-testowe")))
                .isInstanceOf(DisabledException.class);
    }
}
