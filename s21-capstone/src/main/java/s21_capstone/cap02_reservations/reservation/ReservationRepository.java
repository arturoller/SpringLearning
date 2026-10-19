package s21_capstone.cap02_reservations.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s21_capstone.cap02_reservations.desk.Desk;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Repozytorium rezerwacji. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Czy na stanowisku jest rezerwacja o danym statusie NAKŁADAJĄCA SIĘ na [start, end)?
     * Dwa przedziały półotwarte nakładają się, gdy każdy zaczyna się przed końcem drugiego (PYTANIE REKRUTACYJNE):
     * {@code r.startsAt < end AND start < r.endsAt}. Rezerwacje „stykające się” (10–12 i 12–13) się nie nakładają.
     */
    @Query("""
            select count(r) > 0 from Reservation r
            where r.desk = :desk and r.status = :status
              and r.startsAt < :end and :start < r.endsAt""")
    boolean existsOverlapping(@Param("desk") Desk desk, @Param("status") ReservationStatus status,
                              @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Rezerwacje o danym statusie, które zachodzą na przedział [from, to) — do planu dnia stanowiska. */
    @Query("""
            select r from Reservation r
            where r.desk = :desk and r.status = :status
              and r.startsAt < :to and :from < r.endsAt
            order by r.startsAt""")
    List<Reservation> findOverlapping(@Param("desk") Desk desk, @Param("status") ReservationStatus status,
                                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Rezerwacja razem ze stanowiskiem (DTO potrzebuje jego kodu) — jedno zapytanie. */
    @Query("select r from Reservation r join fetch r.desk where r.id = :id")
    Optional<Reservation> findWithDeskById(@Param("id") Long id);
}
