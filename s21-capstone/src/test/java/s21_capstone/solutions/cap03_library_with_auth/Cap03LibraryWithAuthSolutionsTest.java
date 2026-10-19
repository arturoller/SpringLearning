package s21_capstone.solutions.cap03_library_with_auth;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import s21_capstone.cap03_library_with_auth.Cap03Application;
import s21_capstone.cap03_library_with_auth.Cap03Exercises.BookReservationResponse;
import s21_capstone.cap03_library_with_auth.bookreservation.BookReservation;
import s21_capstone.cap03_library_with_auth.bookreservation.BookReservationRepository;
import s21_capstone.cap03_library_with_auth.common.ConflictException;
import s21_capstone.cap03_library_with_auth.common.NotFoundException;
import s21_capstone.cap03_library_with_auth.loan.LoanDtos.LoanResponse;
import s21_capstone.cap03_library_with_auth.loan.LoanService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprawdzenia z Cap03LibraryWithAuthExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze.
 * Pole nazywa się {@code exercises}, żeby treść testów była identyczna jak w testach ćwiczeń.
 * Własna baza ({@code jdbc:h2:mem:cap03-wzorce}) czyszczona przed każdym testem. {@code @WithMockUser} ustawia
 * zalogowanego użytkownika na czas testu — bez niego {@code @PreAuthorize} w serwisach odrzuci każde wywołanie.
 * Dziś = 2026-10-19 (zegar z profilu cap03).
 */
@Tag("wzorzec")
@SpringBootTest(classes = {Cap03Application.class, Cap03LibraryWithAuthSolutions.class},
        properties = {"spring.datasource.url=jdbc:h2:mem:cap03-wzorce", "spring.flyway.clean-disabled=false"})
@ActiveProfiles("cap03")
class Cap03LibraryWithAuthSolutionsTest {

    @Autowired
    Cap03LibraryWithAuthSolutions exercises;

    @Autowired
    LoanService loanService;

    @Autowired
    BookReservationRepository reservations;

    @Autowired
    Flyway flyway;

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
    }

    BookReservation.Status reservationStatus(long id) {
        return reservations.findById(id).orElseThrow().getStatus();
    }

    @Test
    @DisplayName("Wzorzec 1: przedłużenie o 14 dni tylko raz, tylko własne, tylko w terminie i tylko aktywne")
    void solution1() {
        LoanResponse extended = exercises.extend(2, "jan");           // Java. Podstawy, termin 24.10
        assertThat(extended.dueOn()).isEqualTo(LocalDate.of(2026, 11, 7));
        assertThat(extended.extended()).isTrue();

        assertThatThrownBy(() -> exercises.extend(2, "jan"))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("ALREADY_EXTENDED"));
        assertThatThrownBy(() -> exercises.extend(1, "jan"))           // Czysty kod — po terminie
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("LOAN_OVERDUE"));
        assertThatThrownBy(() -> exercises.extend(3, "jan"))           // wypożyczenie Marii
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> exercises.extend(4, "adam"))          // zwrócona w sierpniu
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("ALREADY_RETURNED"));
        assertThatThrownBy(() -> exercises.extend(99, "jan")).isInstanceOf(NotFoundException.class);
    }

    @Test
    @WithMockUser(username = "jan", roles = "CZYTELNIK")
    @DisplayName("Wzorzec 2a: raport zaległości dla czytelnika → AccessDeniedException (403)")
    void solution2ReaderIsDenied() {
        assertThatThrownBy(() -> exercises.overdueReport()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "anna", roles = "BIBLIOTEKARZ")
    @DisplayName("Wzorzec 2b: raport zaległości dla bibliotekarza — najwięcej dni po terminie najpierw, kara na dziś")
    void solution2Report() {
        assertThat(exercises.overdueReport())
                .extracting(row -> row.callNumber() + " " + row.reader() + " " + row.daysOverdue() + " "
                        + row.fine().toPlainString())
                .containsExactly("KSI-003 maria 34 17.00", "KSI-001 jan 4 2.00");
    }

    @Test
    @WithMockUser(username = "anna", roles = "BIBLIOTEKARZ")
    @DisplayName("Wzorzec 3: rezerwacja wypożyczonej książki; po zwrocie dostaje ją tylko rezerwujący (atomowo)")
    void solution3() {
        BookReservationResponse reservation = exercises.reserve(3, "jan");   // Wzorce projektowe ma Maria
        assertThat(reservation.status()).isEqualTo("AKTYWNA");
        assertThatThrownBy(() -> exercises.reserve(3, "adam"))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("ALREADY_RESERVED"));
        assertThatThrownBy(() -> exercises.reserve(3, "maria"))
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("OWN_LOAN"));
        assertThatThrownBy(() -> exercises.reserve(4, "jan"))               // na półce
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("BOOK_AVAILABLE"));

        loanService.returnBook(3);                                          // Maria oddaje
        assertThatThrownBy(() -> exercises.lendRespectingReservations(3, "adam"))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("RESERVED_FOR_OTHER"));
        // Jan ma zaległość (Czysty kod) → lend odmawia → rezerwacja MUSI zostać aktywna (jedna transakcja)
        assertThatThrownBy(() -> exercises.lendRespectingReservations(3, "jan"))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("READER_HAS_OVERDUE"));
        assertThat(reservationStatus(reservation.id())).isEqualTo(BookReservation.Status.AKTYWNA);

        loanService.returnBook(1);                                          // Jan oddaje zaległą książkę
        LoanResponse loan = exercises.lendRespectingReservations(3, "jan");
        assertThat(loan.reader()).isEqualTo("jan");
        assertThat(loan.dueOn()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(reservationStatus(reservation.id())).isEqualTo(BookReservation.Status.ZREALIZOWANA);
    }
}
