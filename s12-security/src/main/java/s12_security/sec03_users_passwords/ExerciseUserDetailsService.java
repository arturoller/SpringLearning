package s12_security.sec03_users_passwords;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

/**
 * SZKIELET ĆWICZENIA 4 lekcji Sec03UsersPasswords: {@code UserDetailsService} nad mapą kont (zamiast bazy — żeby test był
 * zwykłym testem jednostkowym). Wzór: {@link JpaUserDetailsService}.
 */
public class ExerciseUserDetailsService implements UserDetailsService {

    private final Map<String, AppUser> accounts;

    public ExerciseUserDetailsService(Map<String, AppUser> accounts) {
        this.accounts = accounts;
    }

    /**
     * Ćwiczenie 4: zamień konto na {@code UserDetails}: nazwa, hash, role z napisu {@code "CUSTOMER,EMPLOYEE"} (jako role, czyli
     * {@code ROLE_…}), konto wyłączone gdy {@code enabled == false}. Brak konta → {@code UsernameNotFoundException}.
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
