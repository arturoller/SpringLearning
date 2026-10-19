package s21_capstone.cap02_reservations;

import com.jayway.jsonpath.JsonPath;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import s21_capstone.cap02_reservations.notification.Notification;
import s21_capstone.cap02_reservations.notification.NotificationRepository;
import s21_capstone.cap02_reservations.reservation.BookingPolicy;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.NewReservationRequest;
import s21_capstone.cap02_reservations.reservation.ReservationDtos.ReservationResponse;
import s21_capstone.cap02_reservations.reservation.ReservationRepository;
import s21_capstone.cap02_reservations.reservation.ReservationService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy PEŁNEGO KONTEKSTU projektu Cap02Reservations ({@code @SpringBootTest} + MockMvc, profil cap02 z zegarem
 * zatrzymanym na 2026-10-19 10:00). Testy nie są transakcyjne (prawdziwe commity — inaczej słuchacz AFTER_COMMIT nigdy
 * by nie ruszył), więc każdy test rezerwuje INNY dzień — testy nie wchodzą sobie w drogę i nie zależą od kolejności.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("cap02")
class Cap02ReservationsTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ReservationService reservationService;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    BookingPolicy policy;

    @Autowired
    Validator validator;

    /** Szpieg na prawdziwym repozytorium — w teście współbieżności „wciśniemy” drugiego klienta w środek rezerwacji. */
    @MockitoSpyBean
    ReservationRepository reservations;

    static MockHttpServletRequestBuilder reserve(String desk, String startsAt, int hours, String email) {
        return post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content("""
                {"deskCode":"%s","startsAt":"%s","hours":%d,"customerEmail":"%s"}"""
                .formatted(desk, startsAt, hours, email));
    }

    List<String> messagesFor(long reservationId) {
        return notifications.findByReservationIdOrderById(reservationId).stream().map(Notification::getMessage).toList();
    }

    @Test
    @DisplayName("1. Scenariusz: rezerwacja 201 → nakładająca się 409 → styk 201 → anulowanie 200 → ta sama godzina znów 201")
    void reservationLifecycle() throws Exception {
        String created = mockMvc.perform(reserve("SERWIS-1", "2026-10-26T10:00:00", 2, "jan@example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endsAt").value("2026-10-26T12:00:00"))
                .andExpect(jsonPath("$.cancellableUntil").value("2026-10-25T10:00:00"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(reserve("SERWIS-1", "2026-10-26T11:00:00", 1, "ola@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_TAKEN"));
        mockMvc.perform(reserve("SERWIS-1", "2026-10-26T12:00:00", 1, "ola@example.com"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations/" + id + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANULOWANA"));
        mockMvc.perform(post("/api/reservations/" + id + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CANCELLED"))
                .andExpect(jsonPath("$.status").value(409))                       // pole ProblemDetail — nie nadpisane
                .andExpect(jsonPath("$.reservationStatus").value("ANULOWANA"));
        mockMvc.perform(reserve("SERWIS-1", "2026-10-26T11:00:00", 1, "ola@example.com"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("2. Plan dnia SERWIS-1 na dziś: przed 10:00 MINIONY, 15:00 ZAJETY (rezerwacja Zofii), reszta WOLNY")
    void availabilityOfToday() throws Exception {
        mockMvc.perform(get("/api/desks/SERWIS-1/availability").param("date", "2026-10-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(8))
                .andExpect(jsonPath("$.slots[0].start").value("09:00:00"))
                .andExpect(jsonPath("$.slots[0].state").value("MINIONY"))
                .andExpect(jsonPath("$.slots[1].state").value("MINIONY"))     // 10:00 = „teraz”
                .andExpect(jsonPath("$.slots[2].state").value("WOLNY"))
                .andExpect(jsonPath("$.slots[6].state").value("ZAJETY"))      // 15:00–16:00
                .andExpect(jsonPath("$.slots[7].end").value("17:00:00"));
    }

    @Test
    @DisplayName("3. Clock: termin sprzed „teraz” → 422 IN_THE_PAST; anulowanie dzisiejszej rezerwacji → 409 TOO_LATE_TO_CANCEL")
    void clockDrivenRules() throws Exception {
        assertThat(policy.now()).isEqualTo(LocalDateTime.of(2026, 10, 19, 10, 0));
        mockMvc.perform(reserve("SERWIS-2", "2026-10-19T09:00:00", 1, "jan@example.com"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("IN_THE_PAST"));
        mockMvc.perform(post("/api/reservations/2/cancel"))     // Zofia, dziś 15:00 — za 5 godzin
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TOO_LATE_TO_CANCEL"))
                .andExpect(jsonPath("$.cancellableUntil").value("2026-10-18T15:00"));
    }

    /** Pomocniczy rekord do pokazania pułapki: {@code @Future} z Bean Validation. */
    record WithFuture(@Future LocalDateTime startsAt) {
    }

    @Test
    @DisplayName("3. PUŁAPKA: @Future sprawdza zegarem SYSTEMOWYM — 2026-10-19 12:00 jest „przyszłe” dla aplikacji, a dla @Future nie")
    void futureAnnotationIgnoresOurClock() {
        LocalDateTime noonToday = LocalDateTime.of(2026, 10, 19, 12, 0);
        assertThat(noonToday).as("według zegara aplikacji to przyszłość").isAfter(policy.now());

        assertThat(validator.validate(new WithFuture(noonToday)))
                .as("Bean Validation porównuje z prawdziwym czasem komputera (a ten jest już po 19.10.2026 12:00)")
                .hasSize(1);
    }

    @Test
    @DisplayName("4. Współbieżność: drugi klient zapisuje godzinę MIĘDZY naszym sprawdzeniem a INSERT-em → baza zatrzymuje nas, 409")
    void concurrentReservationOfTheSameSlot() throws Exception {
        TransactionTemplate otherClient = new TransactionTemplate(transactionManager);
        otherClient.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AtomicReference<ReservationResponse> competitor = new AtomicReference<>();

        // Gdy NASZA rezerwacja sprawdzi „wolne”, w osobnej transakcji (REQUIRES_NEW = jak drugi wątek, ale
        // deterministycznie) drugi klient rezerwuje nachodzący termin i ZATWIERDZA. Dopiero potem wracamy do nas.
        // Szpieg repozytorium Spring Data to proxy INTERFEJSU: invocation.callRealMethod() rzuca MockitoException
        // „Cannot call abstract real method on java object!”, więc prawdziwe sprawdzenie robimy tym samym warunkiem
        // przez niepodmienioną metodę findOverlapping (szpieg przekazuje ją do prawdziwego repozytorium).
        doAnswer(invocation -> {
            boolean taken = !reservations.findOverlapping(invocation.getArgument(0), invocation.getArgument(1),
                    invocation.getArgument(2), invocation.getArgument(3)).isEmpty();
            if (competitor.get() == null) {
                competitor.set(new ReservationResponse(null, null, null, null, null, null, null));   // znacznik: tylko raz
                competitor.set(otherClient.execute(status -> reservationService.reserve(new NewReservationRequest(
                        "SERWIS-2", LocalDateTime.of(2026, 10, 27, 11, 0), 1, "marek@example.com"))));
            }
            return taken;
        }).when(reservations).existsOverlapping(any(), any(), any(), any());

        mockMvc.perform(reserve("SERWIS-2", "2026-10-27T10:00:00", 2, "jan@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_TAKEN"));

        long competitorId = competitor.get().id();
        mockMvc.perform(get("/api/reservations/" + competitorId))
                .andExpect(jsonPath("$.customerEmail").value("marek@example.com"))
                .andExpect(jsonPath("$.status").value("POTWIERDZONA"));
        mockMvc.perform(get("/api/desks/SERWIS-2/availability").param("date", "2026-10-27"))
                .andExpect(jsonPath("$.slots[1].state").value("WOLNY"))      // 10:00 — nasza rezerwacja NIE powstała
                .andExpect(jsonPath("$.slots[2].state").value("ZAJETY"));    // 11:00 — Marka
        assertThat(messagesFor(competitorId)).hasSize(1);   // potwierdzenie tylko dla zwycięzcy
    }
}
