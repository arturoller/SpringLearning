package s21_capstone.cap02_reservations.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;
import s21_capstone.cap02_reservations.desk.Desk;

import java.time.LocalDateTime;
import java.util.List;

/** Repozytorium listy oczekujących — gotowe pod ĆWICZENIE 3. */
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    /** Kolejka na daną godzinę stanowiska, w kolejności zapisu (id rośnie). */
    List<WaitlistEntry> findByDeskAndSlotStartOrderById(Desk desk, LocalDateTime slotStart);
}
