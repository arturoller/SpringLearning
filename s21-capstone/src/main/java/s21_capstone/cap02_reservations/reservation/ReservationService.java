package s21_capstone.cap02_reservations.reservation;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap02_reservations.common.ConflictException;
import s21_capstone.cap02_reservations.common.NotFoundException;
import s21_capstone.cap02_reservations.desk.Desk;
import s21_capstone.cap02_reservations.desk.DeskRepository;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.AvailabilityResponse;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.DeskResponse;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.NewReservationRequest;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.ReservationResponse;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.SlotState;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.SlotView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Logika rezerwacji. Najważniejsza metoda — {@link #reserve} — ma DWIE linie obrony przed podwójną rezerwacją:
 * <ol>
 *   <li>reguła w serwisie: {@code existsOverlapping} — czytelny 409 SLOT_TAKEN w typowym przypadku;</li>
 *   <li>ograniczenie w bazie: klucz główny occupied_slots (desk_id, slot_start) — gdy dwie transakcje przejdą
 *       sprawdzenie jednocześnie, druga odbije się od bazy, a my zamienimy to na TEN SAM 409.</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservations;
    private final DeskRepository desks;
    private final BookingPolicy policy;
    private final ApplicationEventPublisher events;

    public ReservationService(ReservationRepository reservations, DeskRepository desks, BookingPolicy policy,
                              ApplicationEventPublisher events) {
        this.reservations = reservations;
        this.desks = desks;
        this.policy = policy;
        this.events = events;
    }

    public List<DeskResponse> desks() {
        return desks.findAll().stream().map(DeskResponse::from)
                .sorted((a, b) -> a.code().compareTo(b.code())).toList();
    }

    @Transactional
    public ReservationResponse reserve(NewReservationRequest request) {
        Desk desk = findDesk(request.deskCode());
        policy.checkNewReservation(request.startsAt(), request.hours());
        LocalDateTime start = request.startsAt();
        LocalDateTime end = start.plusHours(request.hours());

        if (reservations.existsOverlapping(desk, ReservationStatus.POTWIERDZONA, start, end)) {
            throw slotTaken(desk, start, end);
        }
        Reservation reservation = new Reservation(desk, request.customerEmail().strip(), start, request.hours(),
                policy.now());
        try {
            reservations.saveAndFlush(reservation);   // INSERT-y TERAZ: naruszenie klucza occupied_slots wyjdzie tutaj
        } catch (DataIntegrityViolationException raceLost) {
            throw slotTaken(desk, start, end);         // ktoś zajął godzinę między sprawdzeniem a zapisem
        }
        // Zdarzenie publikujemy W transakcji, ale słuchacz (ConfirmationNotifier) ruszy dopiero PO commicie.
        events.publishEvent(new ReservationConfirmed(reservation.getId(), reservation.getCustomerEmail(),
                desk.getCode(), start, end));
        return ReservationResponse.from(reservation);
    }

    public ReservationResponse get(long id) {
        return ReservationResponse.from(find(id));
    }

    /** Anulowanie: reguły w encji, „teraz” z zegara. Commit wyśle UPDATE i DELETE zajętych godzin. */
    @Transactional
    public ReservationResponse cancel(long id) {
        Reservation reservation = find(id);
        reservation.cancel(policy.now());
        return ReservationResponse.from(reservation);
    }

    /** Plan dnia stanowiska: każda godzina 9–17 jako WOLNY, ZAJETY albo MINIONY (już nie do zarezerwowania). */
    public AvailabilityResponse availability(String deskCode, LocalDate date) {
        Desk desk = findDesk(deskCode);
        LocalDateTime dayStart = date.atTime(BookingPolicy.OPENS);
        LocalDateTime dayEnd = date.atTime(BookingPolicy.CLOSES);
        List<Reservation> active = reservations.findOverlapping(desk, ReservationStatus.POTWIERDZONA, dayStart, dayEnd);
        LocalDateTime now = policy.now();

        List<SlotView> slots = new ArrayList<>();
        for (LocalDateTime slot = dayStart; slot.isBefore(dayEnd); slot = slot.plusHours(1)) {
            LocalDateTime slotStart = slot;
            SlotState state = !slotStart.isAfter(now) ? SlotState.MINIONY
                    : active.stream().anyMatch(r -> r.covers(slotStart)) ? SlotState.ZAJETY
                    : SlotState.WOLNY;
            slots.add(new SlotView(slotStart.toLocalTime(), slotStart.plusHours(1).toLocalTime(), state));
        }
        return new AvailabilityResponse(desk.getCode(), date, slots);
    }

    private Desk findDesk(String code) {
        return desks.findByCode(code).orElseThrow(() -> new NotFoundException("stanowisko", code));
    }

    private Reservation find(long id) {
        return reservations.findWithDeskById(id).orElseThrow(() -> new NotFoundException("rezerwacja", id));
    }

    private static ConflictException slotTaken(Desk desk, LocalDateTime start, LocalDateTime end) {
        return new ConflictException("SLOT_TAKEN", "Termin na stanowisku " + desk.getCode() + " jest już zajęty",
                Map.of("deskCode", desk.getCode(), "startsAt", start.toString(), "endsAt", end.toString()));
    }
}
