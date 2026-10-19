package s21_capstone.solutions.cap02_reservations;

import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap02_reservations.common.ConflictException;
import s21_capstone.cap02_reservations.common.NotFoundException;
import s21_capstone.cap02_reservations.desk.Desk;
import s21_capstone.cap02_reservations.desk.DeskRepository;
import s21_capstone.cap02_reservations.notification.Notification;
import s21_capstone.cap02_reservations.notification.NotificationRepository;
import s21_capstone.cap02_reservations.reservation.BookingPolicy;
import s21_capstone.cap02_reservations.reservation.OccupiedSlot;
import s21_capstone.cap02_reservations.reservation.Reservation;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.NewReservationRequest;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.ReservationResponse;
import s21_capstone.cap02_reservations.reservation.ReservationRepository;
import s21_capstone.cap02_reservations.reservation.ReservationService;
import s21_capstone.cap02_reservations.reservation.ReservationStatus;
import s21_capstone.cap02_reservations.waitlist.WaitlistEntry;
import s21_capstone.cap02_reservations.waitlist.WaitlistRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rozwiązania wzorcowe ćwiczeń projektu Cap02Reservations — zajrzyj dopiero po własnej próbie!
 * Bean spoza skanowanego pakietu projektu — test rozwiązań rejestruje go w {@code @SpringBootTest(classes = ...)}.
 */
@Service
public class Cap02ReservationsSolutions {

    private static final int MAX_ACTIVE_PER_CUSTOMER = 2;
    private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReservationService reservationService;
    private final ReservationRepository reservations;
    private final DeskRepository desks;
    private final WaitlistRepository waitlist;
    private final NotificationRepository notifications;
    private final BookingPolicy policy;
    private final EntityManager entityManager;

    public Cap02ReservationsSolutions(ReservationService reservationService, ReservationRepository reservations,
                                      DeskRepository desks, WaitlistRepository waitlist,
                                      NotificationRepository notifications, BookingPolicy policy,
                                      EntityManager entityManager) {
        this.reservationService = reservationService;
        this.reservations = reservations;
        this.desks = desks;
        this.waitlist = waitlist;
        this.notifications = notifications;
        this.policy = policy;
        this.entityManager = entityManager;
    }

    /**
     * ĆWICZENIE 1: liczymy aktywne, przyszłe rezerwacje klienta i dopiero potem rezerwujemy — w JEDNEJ transakcji
     * (reserve dołącza do niej). Wyścig dwóch równoczesnych żądań tego samego klienta nadal jest możliwy (check-then-act);
     * w bazie zamknęłaby go np. tabela „licznik rezerwacji klienta” z {@code @Version} albo blokada wiersza klienta.
     */
    @Transactional
    public ReservationResponse reserveWithinLimit(NewReservationRequest request) {
        Long active = entityManager.createQuery("""
                        select count(r) from Reservation r
                        where lower(r.customerEmail) = lower(:email) and r.status = :status and r.startsAt > :now""",
                        Long.class)
                .setParameter("email", request.customerEmail().strip())
                .setParameter("status", ReservationStatus.POTWIERDZONA)
                .setParameter("now", policy.now())
                .getSingleResult();
        if (active >= MAX_ACTIVE_PER_CUSTOMER) {
            throw new ConflictException("RESERVATION_LIMIT",
                    "Klient może mieć najwyżej " + MAX_ACTIVE_PER_CUSTOMER + " aktywne rezerwacje",
                    Map.of("limit", MAX_ACTIVE_PER_CUSTOMER));
        }
        return reservationService.reserve(request);
    }

    /**
     * ĆWICZENIE 2: okno (teraz, teraz + 24 h], tylko bez przypomnienia; po wysłaniu znacznik reminderSentAt — drugie
     * wywołanie nic nie wyśle. {@code @Scheduled} zadziała, gdy aplikacja ma {@code @EnableScheduling}
     * (tu nie ma — test woła metodę wprost).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public int sendReminders() {
        LocalDateTime now = policy.now();
        List<Reservation> due = entityManager.createQuery("""
                        select r from Reservation r join fetch r.desk
                        where r.status = :status and r.reminderSentAt is null
                          and r.startsAt > :now and r.startsAt <= :until
                        order by r.startsAt""", Reservation.class)
                .setParameter("status", ReservationStatus.POTWIERDZONA)
                .setParameter("now", now)
                .setParameter("until", now.plusHours(24))
                .getResultList();
        for (Reservation reservation : due) {
            notifications.save(new Notification(reservation.getId(), reservation.getCustomerEmail(),
                    "Przypomnienie: " + reservation.getStartsAt().format(DAY_TIME) + " stanowisko "
                            + reservation.getDesk().getCode(), now));
            reservation.markReminderSent(now);
        }
        return due.size();
    }

    /** ĆWICZENIE 3a: na listę tylko przy zajętej godzinie; pozycja = liczba wpisów po zapisie. */
    @Transactional
    public int joinWaitlist(String deskCode, LocalDateTime slotStart, String customerEmail) {
        Desk desk = desks.findByCode(deskCode).orElseThrow(() -> new NotFoundException("stanowisko", deskCode));
        if (!reservations.existsOverlapping(desk, ReservationStatus.POTWIERDZONA, slotStart, slotStart.plusHours(1))) {
            throw new ConflictException("SLOT_FREE", "Ta godzina jest wolna — zarezerwuj ją",
                    Map.of("deskCode", deskCode, "slotStart", slotStart.toString()));
        }
        waitlist.save(new WaitlistEntry(desk, slotStart, customerEmail, policy.now()));
        return waitlist.findByDeskAndSlotStartOrderById(desk, slotStart).size();
    }

    /**
     * ĆWICZENIE 3b: jedna transakcja. Godziny zwolnione zapamiętujemy PRZED anulowaniem (cancel czyści zbiór).
     * reservationService.reserve dołącza do tej transakcji; jej zapytanie existsOverlapping (JPQL) wymusza flush —
     * Hibernate najpierw wysyła UPDATE statusu i DELETE z occupied_slots, więc nowa rezerwacja widzi wolne godziny.
     */
    @Transactional
    public List<ReservationResponse> cancelAndPromote(long reservationId) {
        Reservation reservation = reservations.findWithDeskById(reservationId)
                .orElseThrow(() -> new NotFoundException("rezerwacja", reservationId));
        Desk desk = reservation.getDesk();
        List<LocalDateTime> freed = reservation.getOccupiedSlots().stream()
                .map(OccupiedSlot::slotStart).sorted(Comparator.naturalOrder()).toList();

        reservationService.cancel(reservationId);

        List<ReservationResponse> promoted = new ArrayList<>();
        for (LocalDateTime slot : freed) {
            List<WaitlistEntry> queue = waitlist.findByDeskAndSlotStartOrderById(desk, slot);
            if (!queue.isEmpty()) {
                WaitlistEntry first = queue.getFirst();
                promoted.add(reservationService.reserve(
                        new NewReservationRequest(desk.getCode(), slot, 1, first.getCustomerEmail())));
                waitlist.delete(first);
            }
        }
        return promoted;
    }
}
