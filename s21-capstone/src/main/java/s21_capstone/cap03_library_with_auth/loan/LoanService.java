package s21_capstone.cap03_library_with_auth.loan;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap03_library_with_auth.book.Book;
import s21_capstone.cap03_library_with_auth.book.BookRepository;
import s21_capstone.cap03_library_with_auth.common.ConflictException;
import s21_capstone.cap03_library_with_auth.common.NotFoundException;
import s21_capstone.cap03_library_with_auth.loan.LoanDtos.LoanResponse;
import s21_capstone.cap03_library_with_auth.loan.LoanDtos.NewLoanRequest;
import s21_capstone.cap03_library_with_auth.user.LibraryUser;
import s21_capstone.cap03_library_with_auth.user.LibraryUserRepository;
import s21_capstone.cap03_library_with_auth.user.Role;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Wypożyczenia. Uprawnienia na DWÓCH poziomach:
 * <ul>
 *   <li>reguły URL w łańcuchu filtrów (Cap03SecurityConfig) — grubo: „POST /api/loans tylko bibliotekarz”;</li>
 *   <li>{@code @PreAuthorize}/{@code @PostAuthorize} tutaj — dokładnie, z DANYMI: „czytelnik widzi tylko SWOJE
 *       wypożyczenia” (parametr {@code #username} albo wynik {@code returnObject} porównany z {@code authentication.name}).</li>
 * </ul>
 * Adnotacje działają, bo {@code @EnableMethodSecurity} owija ten bean w proxy — wywołanie przez {@code this.metoda()}
 * ominęłoby sprawdzenie (s12_security/Sec02Authorization).
 */
@Service
@Transactional(readOnly = true)
public class LoanService {

    private final LoanRepository loans;
    private final BookRepository books;
    private final LibraryUserRepository users;
    private final FineCalculator fines;
    private final LibraryRules rules;
    private final Clock clock;

    public LoanService(LoanRepository loans, BookRepository books, LibraryUserRepository users, FineCalculator fines,
                       LibraryRules rules, Clock clock) {
        this.loans = loans;
        this.books = books;
        this.users = users;
        this.fines = fines;
        this.rules = rules;
        this.clock = clock;
    }

    /**
     * Wypożyczenie egzemplarza czytelnikowi. Reguły: czytelnik bez zaległości, najwyżej {@code maxActiveLoans} książek,
     * egzemplarz na półce. Termin = dziś + {@code loanDays}. {@code book.markLoaned()} zmienia wiersz książki — przy
     * dwóch równoczesnych wypożyczeniach tego samego egzemplarza @Version zatrzyma drugie (409).
     */
    @Transactional
    @PreAuthorize("hasRole('BIBLIOTEKARZ')")
    public LoanResponse lend(NewLoanRequest request) {
        LocalDate today = LocalDate.now(clock);
        Book book = books.findById(request.bookId()).orElseThrow(() -> new NotFoundException("książka", request.bookId()));
        LibraryUser reader = users.findByUsername(request.reader())
                .filter(user -> user.getRole() == Role.CZYTELNIK)
                .orElseThrow(() -> new NotFoundException("czytelnik", request.reader()));
        if (loans.hasOverdue(reader, today)) {
            throw new ConflictException("READER_HAS_OVERDUE",
                    "Czytelnik " + reader.getUsername() + " ma książki po terminie — najpierw zwrot",
                    Map.of("reader", reader.getUsername()));
        }
        if (loans.countActive(reader) >= rules.maxActiveLoans()) {
            throw new ConflictException("LOAN_LIMIT", "Czytelnik może mieć najwyżej " + rules.maxActiveLoans()
                    + " książki naraz", Map.of("limit", rules.maxActiveLoans()));
        }
        book.markLoaned();
        Loan loan = loans.save(new Loan(book, reader, today, today.plusDays(rules.loanDays())));
        return LoanResponse.from(loan, today, fines);
    }

    /** Zwrot: kara = dni po terminie × stawka; egzemplarz wraca na półkę. */
    @Transactional
    @PreAuthorize("hasRole('BIBLIOTEKARZ')")
    public LoanResponse returnBook(long loanId) {
        LocalDate today = LocalDate.now(clock);
        Loan loan = loans.findDetailedById(loanId).orElseThrow(() -> new NotFoundException("wypożyczenie", loanId));
        loan.markReturned(today, fines.fineFor(loan.getDueOn(), today));
        loan.getBook().markReturned();
        return LoanResponse.from(loan, today, fines);
    }

    /** Wypożyczenia czytelnika: bibliotekarz — dowolnego, czytelnik — tylko SWOJE (#username = parametr metody). */
    @PreAuthorize("hasRole('BIBLIOTEKARZ') or #username == authentication.name")
    public List<LoanResponse> loansOf(String username) {
        LocalDate today = LocalDate.now(clock);
        return loans.findAllOfReader(username).stream().map(loan -> LoanResponse.from(loan, today, fines)).toList();
    }

    /**
     * Jedno wypożyczenie. Właściciela znamy dopiero PO odczycie, więc sprawdzamy WYNIK: {@code @PostAuthorize}
     * (metoda się wykona, ale cudzy wynik nie wyjdzie — 403). Uwaga: przez to nieistniejące id daje 404,
     * a cudze — 403; tak zdradzamy, które id istnieją (zwykle akceptowalne, przy tajnych danych — zwracaj 404).
     */
    @PostAuthorize("hasRole('BIBLIOTEKARZ') or returnObject.reader() == authentication.name")
    public LoanResponse get(long loanId) {
        Loan loan = loans.findDetailedById(loanId).orElseThrow(() -> new NotFoundException("wypożyczenie", loanId));
        return LoanResponse.from(loan, LocalDate.now(clock), fines);
    }
}
