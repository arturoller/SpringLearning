package s21_capstone.cap02_reservations;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import s21_capstone.cap02_reservations.desk.DeskRepository;
import s21_capstone.cap02_reservations.notification.NotificationRepository;
import s21_capstone.cap02_reservations.reservation.BookingPolicy;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.NewReservationRequest;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.ReservationResponse;
import s21_capstone.cap02_reservations.reservation.ReservationRepository;
import s21_capstone.cap02_reservations.reservation.ReservationService;
import s21_capstone.cap02_reservations.waitlist.WaitlistRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ĆWICZENIA projektu Cap02Reservations = ROZSZERZENIA systemu rezerwacji (opis w Javadocu metod). Klasa jest beanem —
 * możesz dopisać {@code @Transactional} i {@code @Scheduled}. Dopóki nie rozwiążesz ćwiczeń, metody rzucają
 * UnsupportedOperationException. Testy: Cap02ReservationsExercisesTest (▶ sam). Rozwiązania:
 * s21_capstone.solutions.cap02_reservations. „Teraz” = {@code policy.now()} (zegar demonstracyjny: 2026-10-19 10:00).
 *
 * <p>Gotowe: kolumna reminder_sent_at i {@code Reservation.markReminderSent}, tabela waitlist_entries z encją
 * WaitlistEntry i repozytorium, {@code ReservationService.reserve/cancel}, NotificationRepository.</p>
 */
@Service
public class Cap02Exercises {

    /** Najwyżej tyle AKTYWNYCH, przyszłych rezerwacji może mieć jeden klient (ĆWICZENIE 1). */
    public static final int MAX_ACTIVE_PER_CUSTOMER = 2;

    private final ReservationService reservationService;
    private final ReservationRepository reservations;
    private final DeskRepository desks;
    private final WaitlistRepository waitlist;
    private final NotificationRepository notifications;
    private final BookingPolicy policy;
    private final EntityManager entityManager;

    public Cap02Exercises(ReservationService reservationService, ReservationRepository reservations,
                          DeskRepository desks, WaitlistRepository waitlist, NotificationRepository notifications,
                          BookingPolicy policy, EntityManager entityManager) {
        this.reservationService = reservationService;
        this.reservations = reservations;
        this.desks = desks;
        this.waitlist = waitlist;
        this.notifications = notifications;
        this.policy = policy;
        this.entityManager = entityManager;
    }

    /**
     * ĆWICZENIE 1 (łatwe): limit rezerwacji na klienta. Jak {@code reservationService.reserve(request)}, ale gdy klient
     * (ten sam email, bez względu na wielkość liter) ma już {@link #MAX_ACTIVE_PER_CUSTOMER} rezerwacje POTWIERDZONE,
     * które jeszcze się nie zaczęły (startsAt po „teraz”) → {@code ConflictException("RESERVATION_LIMIT", ...,
     * Map.of("limit", 2))}. Anulowane i minione się nie liczą.
     * Podpowiedź: zapytanie JPQL z {@code lower(r.customerEmail) = lower(:email)} przez {@code entityManager} albo
     * dopisana metoda w ReservationRepository. Całość w {@code @Transactional}, żeby sprawdzenie i zapis były jedną
     * transakcją (i tak zostaje wyścig dwóch równoczesnych żądań — jak go zamknąć w bazie? Pomyśl o sekcji 4 lekcji).
     */
    public ReservationResponse reserveWithinLimit(NewReservationRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): przypomnienia. Dla każdej rezerwacji POTWIERDZONEJ, która zaczyna się w ciągu najbliższych
     * 24 godzin (startsAt po „teraz” i nie później niż „teraz” + 24 h) i nie ma jeszcze przypomnienia
     * (reminderSentAt == null): zapisz Notification z wiadomością „Przypomnienie: jutro/dziś o HH:mm stanowisko KOD”
     * (dokładny tekst dowolny, ale ZACZYNAJĄCY SIĘ od „Przypomnienie”) i oznacz rezerwację
     * {@code markReminderSent(teraz)}. Zwróć liczbę wysłanych przypomnień. Drugie wywołanie zaraz po pierwszym ma zwrócić
     * 0 (idempotentność — scheduler może uruchomić metodę dwa razy).
     * W aplikacji wołałby ją harmonogram: {@code @Scheduled(cron = "0 0 * * * *")} (co godzinę) + {@code @EnableScheduling}
     * na konfiguracji (s16_cache_scheduling_async/Sched03Scheduled). Test woła metodę wprost — nie czeka na zegar.
     * Podpowiedź: {@code @Transactional}; zmienione encje zapisze commit.
     */
    public int sendReminders() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3a (trudniejsze): zapis na listę oczekujących na godzinę {@code slotStart} stanowiska {@code deskCode}.
     * Wolno tylko, gdy ta godzina jest ZAJĘTA przez aktywną rezerwację (inaczej {@code ConflictException("SLOT_FREE",
     * ...)} — „zarezerwuj ją po prostu”). Zwróć pozycję w kolejce (1 = pierwszy). Nieznane stanowisko → NotFoundException.
     */
    public int joinWaitlist(String deskCode, LocalDateTime slotStart, String customerEmail) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3b (trudniejsze): anulowanie z awansem z listy oczekujących. Anuluj rezerwację (reguły jak w
     * {@code reservationService.cancel}), a potem dla KAŻDEJ zwolnionej godziny: jeśli ktoś na nią czeka, zarezerwuj ją
     * (1 godzina) dla PIERWSZEGO z kolejki i usuń jego wpis. Zwróć listę nowych rezerwacji (kolejność godzin).
     * Wszystko w JEDNEJ transakcji: gdy anulowanie się nie uda (za późno), nikt nie awansuje i kolejka zostaje.
     * Nowe rezerwacje dostaną potwierdzenia (Notification) — przez to samo zdarzenie, co zwykłe rezerwacje.
     * Podpowiedź: {@code reservationService.reserve(...)} wewnątrz tej samej transakcji zobaczy już zwolnione godziny?
     * Sprawdź, kiedy Hibernate wysyła DELETE z occupied_slots (flush przed zapytaniem JPQL — tryb AUTO).
     */
    public List<ReservationResponse> cancelAndPromote(long reservationId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
