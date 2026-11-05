package s12_security.sec03_users_passwords;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repozytorium użytkowników lekcji Sec03UsersPasswords (Spring Data JPA, s08_data_jpa/Jpa03QueryMethods). */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
