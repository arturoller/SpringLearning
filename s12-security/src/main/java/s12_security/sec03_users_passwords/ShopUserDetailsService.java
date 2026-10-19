package s12_security.sec03_users_passwords;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * ShopUserDetailsService = WŁASNA implementacja {@code UserDetailsService} (usługa danych użytkownika):
 * tłumaczy konto z naszej „bazy” (EmployeeAccount) na obiekt, który rozumie Spring Security ({@code UserDetails}).
 *
 * <p>Spring Security NIE sprawdza tu hasła. Ta klasa odpowiada tylko na pytanie „kim jest X i jaki ma skrót
 * hasła?”. Porównanie wpisanego hasła ze skrótem robi DaoAuthenticationProvider za pomocą PasswordEncoder.</p>
 */
@Service
public class ShopUserDetailsService implements UserDetailsService {

    private final EmployeeAccountRepository repository;

    public ShopUserDetailsService(EmployeeAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        EmployeeAccount account = repository.findByUsername(username)
                // username not found = nie znaleziono użytkownika; Spring i tak pokaże klientowi tylko „Bad credentials”
                .orElseThrow(() -> new UsernameNotFoundException("Nie ma użytkownika: " + username));
        return User.withUsername(account.username())
                .password(account.passwordHash())                    // SKRÓT hasła, nie hasło
                .roles(account.roles().toArray(String[]::new))       // ADMIN → ROLE_ADMIN
                .disabled(!account.active())                         // disabled = wyłączone (zablokowane konto)
                .build();
    }
}
