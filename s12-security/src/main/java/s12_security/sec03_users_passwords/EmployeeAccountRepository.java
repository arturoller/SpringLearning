package s12_security.sec03_users_passwords;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EmployeeAccountRepository = „baza” kont pracowników — tu zwykła mapa w pamięci (w tym module nie ma JPA;
 * repozytorium na bazie danych: s08_data_jpa/Jpa02Repositories).
 *
 * <p>Hasła TESTOWE są kodowane przy starcie przez bean PasswordEncoder. W prawdziwym systemie hasło koduje się
 * JEDEN raz — gdy użytkownik je ustawia — a w bazie leży tylko skrót.</p>
 */
@Repository   // repository = repozytorium (dostęp do danych)
public class EmployeeAccountRepository {

    private final Map<String, EmployeeAccount> accounts = new ConcurrentHashMap<>();

    public EmployeeAccountRepository(PasswordEncoder passwordEncoder) {
        save(new EmployeeAccount("anna", passwordEncoder.encode("anna-test-123"), List.of("ADMIN"), true));
        save(new EmployeeAccount("piotr", passwordEncoder.encode("piotr-test-123"), List.of("USER"), true));
        // marek odszedł z firmy — konto zablokowane, ale wciąż w bazie
        save(new EmployeeAccount("marek", passwordEncoder.encode("marek-test-123"), List.of("USER"), false));
    }

    public Optional<EmployeeAccount> findByUsername(String username) {
        return Optional.ofNullable(accounts.get(username));
    }

    public void save(EmployeeAccount account) {
        accounts.put(account.username(), account);
    }
}
