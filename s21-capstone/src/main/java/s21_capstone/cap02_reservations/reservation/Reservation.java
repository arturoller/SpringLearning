package s21_capstone.cap02_reservations.reservation;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import s21_capstone.cap02_reservations.common.ConflictException;
import s21_capstone.cap02_reservations.desk.Desk;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rezerwacja stanowiska na przedział [startsAt, endsAt) — pełne godziny, 1–3 godziny.
 *
 * <p>Każda godzina AKTYWNEJ rezerwacji to wpis w {@code occupiedSlots} (tabela occupied_slots). Klucz główny tej
 * tabeli (desk_id, slot_start) pilnuje w BAZIE, żeby dwie rezerwacje nie zajęły tej samej godziny — nawet gdy dwie
 * transakcje jednocześnie przejdą sprawdzenie w serwisie. Anulowanie czyści zbiór, więc Hibernate usuwa wiersze
 * i godziny znów są wolne, a sama rezerwacja zostaje w historii ze statusem ANULOWANA.</p>
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    /** Najpóźniej na tyle przed początkiem można anulować. */
    public static final Duration CANCELLATION_NOTICE = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desk_id")
    private Desk desk;

    @Column(name = "customer_email", nullable = false, length = 100)
    private String customerEmail;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** Pod ĆWICZENIE 2 (przypomnienia): kiedy wysłano przypomnienie; null = jeszcze nie. */
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    /** element collection = kolekcja wartości (nie encji) w osobnej tabeli, zapisywana i usuwana razem z rezerwacją. */
    @ElementCollection
    @CollectionTable(name = "occupied_slots", joinColumns = @JoinColumn(name = "reservation_id"))
    private Set<OccupiedSlot> occupiedSlots = new HashSet<>();

    protected Reservation() {
    }

    public Reservation(Desk desk, String customerEmail, LocalDateTime startsAt, int hours, LocalDateTime createdAt) {
        this.desk = desk;
        this.customerEmail = customerEmail;
        this.startsAt = startsAt;
        this.endsAt = startsAt.plusHours(hours);
        this.status = ReservationStatus.POTWIERDZONA;
        this.createdAt = createdAt;
        for (int hour = 0; hour < hours; hour++) {
            occupiedSlots.add(new OccupiedSlot(desk.getId(), startsAt.plusHours(hour)));
        }
    }

    /** Do kiedy wolno anulować: początek minus 24 godziny. */
    public LocalDateTime cancellableUntil() {
        return startsAt.minus(CANCELLATION_NOTICE);
    }

    /**
     * Anulowanie: tylko rezerwacja POTWIERDZONA i najpóźniej {@link #CANCELLATION_NOTICE} przed początkiem.
     * Czas „teraz” podaje wołający (z zegara) — encja nie sięga po {@code LocalDateTime.now()}.
     */
    public void cancel(LocalDateTime now) {
        if (status == ReservationStatus.ANULOWANA) {
            throw new ConflictException("ALREADY_CANCELLED", "Rezerwacja jest już anulowana",
                    Map.of("reservationStatus", status.name()));
        }
        if (now.isAfter(cancellableUntil())) {
            throw new ConflictException("TOO_LATE_TO_CANCEL",
                    "Rezerwację można anulować najpóźniej 24 godziny przed początkiem",
                    Map.of("cancellableUntil", cancellableUntil().toString()));
        }
        status = ReservationStatus.ANULOWANA;
        cancelledAt = now;
        occupiedSlots.clear();   // Hibernate usunie wiersze z occupied_slots — godziny znów wolne
    }

    /** Czy rezerwacja (aktywna) obejmuje godzinę zaczynającą się o {@code slotStart}. */
    public boolean covers(LocalDateTime slotStart) {
        return status == ReservationStatus.POTWIERDZONA && !slotStart.isBefore(startsAt) && slotStart.isBefore(endsAt);
    }

    /** Pod ĆWICZENIE 2: zapamiętaj, że przypomnienie wysłano. */
    public void markReminderSent(LocalDateTime now) {
        this.reminderSentAt = now;
    }

    public Long getId() {
        return id;
    }

    public Desk getDesk() {
        return desk;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public Set<OccupiedSlot> getOccupiedSlots() {
        return Collections.unmodifiableSet(occupiedSlots);
    }
}
