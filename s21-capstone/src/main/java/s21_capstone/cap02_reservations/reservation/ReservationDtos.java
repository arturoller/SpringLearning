package s21_capstone.cap02_reservations.reservation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import s21_capstone.cap02_reservations.desk.Desk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** DTO API rezerwacji. */
public final class ReservationDtos {

    private ReservationDtos() {
    }

    /**
     * POST /api/reservations. Celowo BEZ {@code @Future} na startsAt: Bean Validation sprawdza „przyszłość” zegarem
     * SYSTEMOWYM, a nie naszym beanem {@code Clock} (Cap02ReservationsTest, sekcja 3). Regułę „nie w przeszłości”
     * sprawdza BookingPolicy — tym samym zegarem, co reszta aplikacji.
     */
    public record NewReservationRequest(
            @NotBlank(message = "kod stanowiska jest wymagany")
            String deskCode,
            @NotNull(message = "początek jest wymagany")
            LocalDateTime startsAt,
            @NotNull(message = "liczba godzin jest wymagana")
            @Min(value = 1, message = "od 1 do 3 godzin")
            @Max(value = 3, message = "od 1 do 3 godzin")
            Integer hours,
            @NotBlank(message = "email jest wymagany")
            @Email(message = "niepoprawny adres email")
            String customerEmail) {
    }

    public record ReservationResponse(Long id, String deskCode, String customerEmail, LocalDateTime startsAt,
                                      LocalDateTime endsAt, ReservationStatus status, LocalDateTime cancellableUntil) {

        public static ReservationResponse from(Reservation reservation) {
            return new ReservationResponse(reservation.getId(), reservation.getDesk().getCode(),
                    reservation.getCustomerEmail(), reservation.getStartsAt(), reservation.getEndsAt(),
                    reservation.getStatus(), reservation.cancellableUntil());
        }
    }

    public record DeskResponse(String code, String name) {

        public static DeskResponse from(Desk desk) {
            return new DeskResponse(desk.getCode(), desk.getName());
        }
    }

    /** Stan jednej godziny w planie dnia. */
    public enum SlotState {
        WOLNY, ZAJETY, MINIONY
    }

    public record SlotView(LocalTime start, LocalTime end, SlotState state) {
    }

    /** Plan dnia stanowiska — bez danych klientów (kto zarezerwował, to nie sprawa innych klientów). */
    public record AvailabilityResponse(String deskCode, LocalDate date, List<SlotView> slots) {
    }
}
