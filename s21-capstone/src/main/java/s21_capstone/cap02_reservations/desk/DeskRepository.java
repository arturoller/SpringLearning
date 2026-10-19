package s21_capstone.cap02_reservations.desk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repozytorium stanowisk. */
public interface DeskRepository extends JpaRepository<Desk, Long> {

    Optional<Desk> findByCode(String code);
}
