package s12_security.sec03_users_passwords;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Użytkownik sklepu zapisany w bazie (tabela {@code app_users} z migracji Flyway V1). To ENCJA — obiekt bazy, nie odpowiedź
 * API: ma pole {@code passwordHash}, które nigdy nie może trafić do JSON-a (sekcja 5, test Sec03PasswordLeakTest).
 * Do klienta wysyłamy {@link UserView}.
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Role bez prefiksu ROLE_, oddzielone przecinkami, np. {@code "CUSTOMER,EMPLOYEE"}. */
    @Column(nullable = false, length = 200)
    private String roles;

    @Column(nullable = false)
    private boolean enabled;

    protected AppUser() {
        // wymagany przez JPA (Hibernate tworzy obiekt, a potem wypełnia pola)
    }

    public AppUser(String username, String passwordHash, String roles, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRoles() {
        return roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
