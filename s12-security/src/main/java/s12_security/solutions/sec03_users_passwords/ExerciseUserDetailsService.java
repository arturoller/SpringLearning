package s12_security.solutions.sec03_users_passwords;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import s12_security.sec03_users_passwords.AppUser;

import java.util.Map;

/** Rozwiązanie wzorcowe ćwiczenia 4 lekcji Sec03UsersPasswords. */
public class ExerciseUserDetailsService implements UserDetailsService {

    private final Map<String, AppUser> accounts;

    public ExerciseUserDetailsService(Map<String, AppUser> accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser account = accounts.get(username);
        if (account == null) {
            throw new UsernameNotFoundException("Nie ma użytkownika " + username);
        }
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .roles(account.getRoles().split(","))
                .disabled(!account.isEnabled())
                .build();
    }
}
