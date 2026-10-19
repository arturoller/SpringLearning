package s21_capstone.cap03_library_with_auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repozytorium użytkowników biblioteki. */
public interface LibraryUserRepository extends JpaRepository<LibraryUser, Long> {

    Optional<LibraryUser> findByUsername(String username);
}
