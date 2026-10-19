package s21_capstone.cap02_reservations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import s21_capstone.cap02_reservations.common.ConflictException;
import s21_capstone.cap02_reservations.common.RuleViolationException;
import s21_capstone.cap02_reservations.reservation.ReservationController;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.ReservationResponse;
import s21_capstone.cap02_reservations.reservation.ReservationService;
import s21_capstone.cap02_reservations.reservation.ReservationStatus;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wycinek webowy projektu Cap02Reservations: kontroler + porada błędów + nasz łańcuch Security (z Cap02Config),
 * serwis jako atrapa. Sprawdzamy mapowanie: walidacja → 400, reguła czasu → 422, konflikt → 409, sukces → 201.
 */
@WebMvcTest(ReservationController.class)
@Import(Cap02Config.class)
class Cap02ReservationsWebSliceTest {

    static final String VALID = """
            {"deskCode":"SERWIS-1","startsAt":"2026-10-21T12:00:00","hours":2,"customerEmail":"jan@example.com"}""";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReservationService reservationService;

    @Test
    @DisplayName("1. Walidacja żądania: brak początku, 4 godziny i zły email → 400 z mapą errors")
    void validation() throws Exception {
        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"deskCode":"SERWIS-1","hours":4,"customerEmail":"nie-email"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.startsAt").value("początek jest wymagany"))
                .andExpect(jsonPath("$.errors.hours").value("od 1 do 3 godzin"))
                .andExpect(jsonPath("$.errors.customerEmail").value("niepoprawny adres email"));
        verifyNoInteractions(reservationService);
    }

    @Test
    @DisplayName("3. Reguła czasu z serwisu (RuleViolationException) → 422 Unprocessable Content")
    void ruleViolationIs422() throws Exception {
        when(reservationService.reserve(any())).thenThrow(new RuleViolationException("IN_THE_PAST",
                "Nie można rezerwować terminu, który już minął", Map.of("startsAt", "2026-10-19T09:00")));
        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("IN_THE_PAST"))
                .andExpect(jsonPath("$.type").value("https://serwis.example/problemy/in-the-past"));
    }

    @Test
    @DisplayName("4. Zajęty termin (ConflictException) → 409 z kodem SLOT_TAKEN i godzinami")
    void conflictIs409() throws Exception {
        when(reservationService.reserve(any())).thenThrow(new ConflictException("SLOT_TAKEN",
                "Termin na stanowisku SERWIS-1 jest już zajęty",
                Map.of("deskCode", "SERWIS-1", "startsAt", "2026-10-21T12:00", "endsAt", "2026-10-21T14:00")));
        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_TAKEN"))
                .andExpect(jsonPath("$.endsAt").value("2026-10-21T14:00"));
    }

    @Test
    @DisplayName("1. Sukces → 201 + Location; daty w JSON-ie w formacie ISO (Jackson 3 domyślnie nie pisze liczb)")
    void createdWithLocation() throws Exception {
        when(reservationService.reserve(any())).thenReturn(new ReservationResponse(42L, "SERWIS-1", "jan@example.com",
                LocalDateTime.of(2026, 10, 21, 12, 0), LocalDateTime.of(2026, 10, 21, 14, 0),
                ReservationStatus.POTWIERDZONA, LocalDateTime.of(2026, 10, 20, 12, 0)));
        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/reservations/42"))
                .andExpect(jsonPath("$.startsAt").value("2026-10-21T12:00:00"))
                .andExpect(jsonPath("$.cancellableUntil").value("2026-10-20T12:00:00"));
    }

    @Test
    @DisplayName("1. Plan dnia: data w złym formacie → 400 z klasy bazowej porady (konwersja parametru)")
    void badDateParameter() throws Exception {
        mockMvc.perform(get("/api/desks/SERWIS-1/availability").param("date", "21.10.2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Failed to convert 'date' with value: '21.10.2026'"));
    }
}
