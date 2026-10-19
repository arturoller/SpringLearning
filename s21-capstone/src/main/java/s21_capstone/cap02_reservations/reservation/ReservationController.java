package s21_capstone.cap02_reservations.reservation;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.AvailabilityResponse;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.DeskResponse;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.NewReservationRequest;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.ReservationResponse;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * API rezerwacji.
 * <pre>
 *   GET  /api/desks                                     200 lista stanowisk
 *   GET  /api/desks/{code}/availability?date=2026-10-21 200 plan dnia (WOLNY / ZAJETY / MINIONY) / 404
 *   POST /api/reservations                              201 + Location / 400 / 404 / 409 SLOT_TAKEN / 422 reguła czasu
 *   GET  /api/reservations/{id}                         200 / 404
 *   POST /api/reservations/{id}/cancel                  200 (status ANULOWANA) / 404 / 409
 * </pre>
 * Anulowanie to POST na akcję {@code /cancel}, a nie DELETE: rezerwacja NIE znika (zostaje w historii ze statusem
 * ANULOWANA), zmienia tylko stan — a DELETE obiecuje klientowi, że zasobu już nie będzie.
 */
@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/desks")
    public List<DeskResponse> desks() {
        return reservationService.desks();
    }

    /** {@code @DateTimeFormat(iso = DATE)} — parametr w formacie 2026-10-21 (zły format → 400 z klasy bazowej porady). */
    @GetMapping("/desks/{code}/availability")
    public AvailabilityResponse availability(@PathVariable String code,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.availability(code, date);
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody NewReservationRequest request) {
        ReservationResponse created = reservationService.reserve(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse one(@PathVariable long id) {
        return reservationService.get(id);
    }

    @PostMapping("/reservations/{id}/cancel")
    public ReservationResponse cancel(@PathVariable long id) {
        return reservationService.cancel(id);
    }
}
