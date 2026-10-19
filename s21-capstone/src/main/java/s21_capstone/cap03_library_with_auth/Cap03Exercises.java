package s21_capstone.cap03_library_with_auth;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import s21_capstone.cap03_library_with_auth.book.BookRepository;
import s21_capstone.cap03_library_with_auth.bookreservation.BookReservationRepository;
import s21_capstone.cap03_library_with_auth.loan.FineCalculator;
import s21_capstone.cap03_library_with_auth.loan.LibraryRules;
import s21_capstone.cap03_library_with_auth.loan.LoanDtos.LoanResponse;
import s21_capstone.cap03_library_with_auth.loan.LoanRepository;
import s21_capstone.cap03_library_with_auth.loan.LoanService;
import s21_capstone.cap03_library_with_auth.user.LibraryUserRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * ĆWICZENIA projektu Cap03LibraryWithAuth = ROZSZERZENIA biblioteki (opis w Javadocu metod). Klasa jest beanem —
 * możesz dopisać {@code @Transactional} i {@code @PreAuthorize} (działa {@code @EnableMethodSecurity} z
 * Cap03SecurityConfig). Dopóki nie rozwiążesz ćwiczeń, metody rzucają UnsupportedOperationException.
 * Testy: Cap03LibraryWithAuthExercisesTest (▶ sam). Rozwiązania: s21_capstone.solutions.cap03_library_with_auth.
 * „Dziś” = {@code LocalDate.now(clock)} = 2026-10-19 (zegar demonstracyjny).
 *
 * <p>Gotowe: kolumna loans.extended i {@code Loan.extendBy(dni)}, tabela book_reservations z encją BookReservation
 * i repozytorium ({@code findActiveFor}), {@code FineCalculator}, {@code LibraryRules}, {@code LoanService.lend}.</p>
 */
@Service
public class Cap03Exercises {

    /** Wiersz raportu zaległości (ĆWICZENIE 2). */
    public record OverdueLoan(String callNumber, String title, String reader, LocalDate dueOn, long daysOverdue,
                              BigDecimal fine) {
    }

    /** Rezerwacja książki w odpowiedzi (ĆWICZENIE 3). */
    public record BookReservationResponse(Long id, String callNumber, String reader, LocalDate createdOn,
                                          String status) {
    }

    private final LoanService loanService;
    private final LoanRepository loans;
    private final BookRepository books;
    private final LibraryUserRepository users;
    private final BookReservationRepository reservations;
    private final FineCalculator fines;
    private final LibraryRules rules;
    private final EntityManager entityManager;
    private final Clock clock;

    public Cap03Exercises(LoanService loanService, LoanRepository loans, BookRepository books,
                          LibraryUserRepository users, BookReservationRepository reservations, FineCalculator fines,
                          LibraryRules rules, EntityManager entityManager, Clock clock) {
        this.loanService = loanService;
        this.loans = loans;
        this.books = books;
        this.users = users;
        this.reservations = reservations;
        this.fines = fines;
        this.rules = rules;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /**
     * ĆWICZENIE 1 (łatwe): przedłużenie terminu wypożyczenia {@code loanId} przez czytelnika {@code username}.
     * <ul>
     *   <li>tylko właściciel: cudze → {@code org.springframework.security.access.AccessDeniedException}
     *       („To nie twoje wypożyczenie”) — porada zamieni to na 403;</li>
     *   <li>tylko aktywne (zwrócone → {@code ConflictException("ALREADY_RETURNED", ...)});</li>
     *   <li>nie po terminie (dueOn przed dziś → {@code ConflictException("LOAN_OVERDUE", ...)}) — najpierw zwrot i kara;</li>
     *   <li>najwyżej RAZ (extended → {@code ConflictException("ALREADY_EXTENDED", ...)});</li>
     *   <li>termin + {@code rules.loanDays()} dni ({@code loan.extendBy(...)}); zwróć
     *       {@code LoanResponse.from(loan, dziś, fines)}.</li>
     * </ul>
     * Podpowiedź: {@code @Transactional}, {@code loans.findDetailedById}. Nieznane id → NotFoundException.
     */
    public LoanResponse extend(long loanId, String username) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): raport zaległości — TYLKO dla bibliotekarza. Wszystkie NIEZWRÓCONE wypożyczenia
     * z terminem przed dziś: sygnatura, tytuł, login czytelnika, termin, dni po terminie i kara na dziś
     * ({@code fines.daysOverdue}, {@code fines.fineFor}). Kolejność: najwięcej dni po terminie najpierw.
     * Dla danych startowych: KSI-003 maria 34 dni 17.00 · KSI-001 jan 4 dni 2.00.
     * Czytelnik wołający tę metodę ma dostać AccessDeniedException (403) — dopisz adnotację, która to zapewni.
     * Podpowiedź: {@code @PreAuthorize("hasRole('BIBLIOTEKARZ')")} + jedno zapytanie JPQL z {@code join fetch}
     * (bez N+1 przy odczycie tytułu i czytelnika).
     */
    public List<OverdueLoan> overdueReport() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3a (trudniejsze): rezerwacja WYPOŻYCZONEGO egzemplarza {@code bookId} przez czytelnika {@code username}.
     * <ul>
     *   <li>egzemplarz na półce → {@code ConflictException("BOOK_AVAILABLE", ...)} („wypożycz go po prostu”);</li>
     *   <li>czytelnik, który TEN egzemplarz właśnie ma → {@code ConflictException("OWN_LOAN", ...)};</li>
     *   <li>już jest aktywna rezerwacja (kogokolwiek) → {@code ConflictException("ALREADY_RESERVED", ...)};</li>
     *   <li>zapisz {@code new BookReservation(book, reader, dziś)}; zwróć BookReservationResponse (status "AKTYWNA").</li>
     * </ul>
     */
    public BookReservationResponse reserve(long bookId, String username) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3b (trudniejsze): wypożyczenie z poszanowaniem rezerwacji. Jak {@code loanService.lend}, ale gdy
     * egzemplarz ma AKTYWNĄ rezerwację: innego czytelnika → {@code ConflictException("RESERVED_FOR_OTHER", ...)};
     * tego samego → wypożycz i oznacz rezerwację {@code markFulfilled()} — w JEDNEJ transakcji (gdy lend rzuci wyjątek,
     * rezerwacja ma zostać AKTYWNA). Wołający musi być bibliotekarzem — lend już to sprawdza ({@code @PreAuthorize}).
     */
    public LoanResponse lendRespectingReservations(long bookId, String readerUsername) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
