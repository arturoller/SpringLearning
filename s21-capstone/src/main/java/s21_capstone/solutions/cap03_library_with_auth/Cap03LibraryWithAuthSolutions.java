package s21_capstone.solutions.cap03_library_with_auth;

import jakarta.persistence.EntityManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap03_library_with_auth.Cap03Exercises.BookReservationResponse;
import s21_capstone.cap03_library_with_auth.Cap03Exercises.OverdueLoan;
import s21_capstone.cap03_library_with_auth.book.Book;
import s21_capstone.cap03_library_with_auth.book.BookRepository;
import s21_capstone.cap03_library_with_auth.bookreservation.BookReservation;
import s21_capstone.cap03_library_with_auth.bookreservation.BookReservationRepository;
import s21_capstone.cap03_library_with_auth.common.ConflictException;
import s21_capstone.cap03_library_with_auth.common.NotFoundException;
import s21_capstone.cap03_library_with_auth.loan.FineCalculator;
import s21_capstone.cap03_library_with_auth.loan.LibraryRules;
import s21_capstone.cap03_library_with_auth.loan.Loan;
import s21_capstone.cap03_library_with_auth.loan.LoanDtos.LoanResponse;
import s21_capstone.cap03_library_with_auth.loan.LoanDtos.NewLoanRequest;
import s21_capstone.cap03_library_with_auth.loan.LoanRepository;
import s21_capstone.cap03_library_with_auth.loan.LoanService;
import s21_capstone.cap03_library_with_auth.user.LibraryUser;
import s21_capstone.cap03_library_with_auth.user.LibraryUserRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rozwiązania wzorcowe ćwiczeń projektu Cap03LibraryWithAuth — zajrzyj dopiero po własnej próbie!
 * Bean spoza skanowanego pakietu projektu — test rozwiązań rejestruje go w {@code @SpringBootTest(classes = ...)};
 * {@code @EnableMethodSecurity} z Cap03SecurityConfig obejmuje także ten bean ({@code @PreAuthorize} w ćwiczeniu 2).
 */
@Service
public class Cap03LibraryWithAuthSolutions {

    private final LoanService loanService;
    private final LoanRepository loans;
    private final BookRepository books;
    private final LibraryUserRepository users;
    private final BookReservationRepository reservations;
    private final FineCalculator fines;
    private final LibraryRules rules;
    private final EntityManager entityManager;
    private final Clock clock;

    public Cap03LibraryWithAuthSolutions(LoanService loanService, LoanRepository loans, BookRepository books,
                                         LibraryUserRepository users, BookReservationRepository reservations,
                                         FineCalculator fines, LibraryRules rules, EntityManager entityManager,
                                         Clock clock) {
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

    /** ĆWICZENIE 1: kolejność sprawdzeń = od „kto pyta” do „czy wolno teraz”. Zmianę zapisze commit. */
    @Transactional
    public LoanResponse extend(long loanId, String username) {
        LocalDate today = LocalDate.now(clock);
        Loan loan = loans.findDetailedById(loanId).orElseThrow(() -> new NotFoundException("wypożyczenie", loanId));
        if (!loan.getReader().getUsername().equals(username)) {
            throw new AccessDeniedException("To nie twoje wypożyczenie");
        }
        if (!loan.isActive()) {
            throw new ConflictException("ALREADY_RETURNED", "Wypożyczenie jest zakończone", Map.of("loanId", loanId));
        }
        if (loan.getDueOn().isBefore(today)) {
            throw new ConflictException("LOAN_OVERDUE", "Termin minął " + loan.getDueOn() + " — najpierw zwrot",
                    Map.of("dueOn", loan.getDueOn().toString()));
        }
        if (loan.isExtended()) {
            throw new ConflictException("ALREADY_EXTENDED", "Termin można przedłużyć tylko raz", Map.of("loanId", loanId));
        }
        loan.extendBy(rules.loanDays());
        return LoanResponse.from(loan, today, fines);
    }

    /** ĆWICZENIE 2: @PreAuthorize na metodzie beana (proxy!) + jedno zapytanie z join fetch. */
    @PreAuthorize("hasRole('BIBLIOTEKARZ')")
    @Transactional(readOnly = true)
    public List<OverdueLoan> overdueReport() {
        LocalDate today = LocalDate.now(clock);
        return entityManager.createQuery("""
                        select l from Loan l join fetch l.book join fetch l.reader
                        where l.returnedOn is null and l.dueOn < :today""", Loan.class)
                .setParameter("today", today)
                .getResultList().stream()
                .map(loan -> new OverdueLoan(loan.getBook().getCallNumber(), loan.getBook().getTitle(),
                        loan.getReader().getUsername(), loan.getDueOn(), fines.daysOverdue(loan.getDueOn(), today),
                        fines.fineFor(loan.getDueOn(), today)))
                .sorted(Comparator.comparingLong(OverdueLoan::daysOverdue).reversed())
                .toList();
    }

    /** ĆWICZENIE 3a: rezerwować można tylko egzemplarz, który ktoś INNY ma teraz w domu. */
    @Transactional
    public BookReservationResponse reserve(long bookId, String username) {
        Book book = books.findById(bookId).orElseThrow(() -> new NotFoundException("książka", bookId));
        LibraryUser reader = users.findByUsername(username).orElseThrow(() -> new NotFoundException("czytelnik", username));
        if (!book.isOnLoan()) {
            throw new ConflictException("BOOK_AVAILABLE", "Egzemplarz jest na półce — wypożycz go",
                    Map.of("callNumber", book.getCallNumber()));
        }
        boolean ownLoan = loans.findAllOfReader(username).stream()
                .anyMatch(loan -> loan.isActive() && loan.getBook().getId().equals(bookId));
        if (ownLoan) {
            throw new ConflictException("OWN_LOAN", "Ten egzemplarz jest wypożyczony przez ciebie",
                    Map.of("callNumber", book.getCallNumber()));
        }
        if (reservations.findActiveFor(book).isPresent()) {
            throw new ConflictException("ALREADY_RESERVED", "Egzemplarz jest już zarezerwowany",
                    Map.of("callNumber", book.getCallNumber()));
        }
        BookReservation reservation = reservations.save(new BookReservation(book, reader, LocalDate.now(clock)));
        return new BookReservationResponse(reservation.getId(), book.getCallNumber(), username,
                reservation.getCreatedOn(), reservation.getStatus().name());
    }

    /**
     * ĆWICZENIE 3b: jedna transakcja — lend dołącza do niej (REQUIRED). Gdy lend rzuci wyjątek (np. zaległości),
     * cała transakcja się wycofa i rezerwacja zostanie AKTYWNA, choć markFulfilled() byłoby już wywołane.
     */
    @Transactional
    public LoanResponse lendRespectingReservations(long bookId, String readerUsername) {
        Book book = books.findById(bookId).orElseThrow(() -> new NotFoundException("książka", bookId));
        Optional<BookReservation> active = reservations.findActiveFor(book);
        if (active.isPresent() && !active.get().getReader().getUsername().equals(readerUsername)) {
            throw new ConflictException("RESERVED_FOR_OTHER", "Egzemplarz czeka na innego czytelnika",
                    Map.of("callNumber", book.getCallNumber()));
        }
        active.ifPresent(BookReservation::markFulfilled);
        return loanService.lend(new NewLoanRequest(bookId, readerUsername));
    }
}
