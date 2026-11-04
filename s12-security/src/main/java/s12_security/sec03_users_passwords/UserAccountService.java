package s12_security.sec03_users_passwords;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Konta użytkowników (sekcje 3 i 4 lekcji Sec03UsersPasswords): rejestracja i zmiana hasła. Haszowanie jest TUTAJ, w serwisie
 * — kontroler nigdy nie dotyka {@code PasswordEncoder}, a encja nigdy nie widzi hasła jawnym tekstem.
 */
@Service
public class UserAccountService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserView register(RegistrationRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new UsernameTakenException(request.username());
        }
        String hash = passwordEncoder.encode(request.password());          // {bcrypt}$2a$10$… — za każdym razem inny (sól)
        try {
            AppUser saved = users.saveAndFlush(new AppUser(request.username(), hash, "CUSTOMER", true));
            return toView(saved);
        } catch (DataIntegrityViolationException raceLost) {
            // Dwie rejestracje tej samej nazwy w tej samej chwili: obie przeszły existsByUsername, ale UNIQUE w bazie
            // przepuściło tylko jedną. Dla klienta to ten sam błąd co wyżej.
            throw new UsernameTakenException(request.username());
        }
    }

    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        AppUser user = users.findByUsername(username).orElseThrow();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new WrongPasswordException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));  // zmiana zapisze się przy końcu transakcji
    }

    static UserView toView(AppUser user) {
        List<String> roles = Arrays.stream(user.getRoles().split(",")).sorted().toList();
        return new UserView(user.getUsername(), roles);
    }
}
