package s21_capstone.cap03_library_with_auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import s21_capstone.cap03_library_with_auth.book.Book;
import s21_capstone.cap03_library_with_auth.common.ConflictException;
import s21_capstone.cap03_library_with_auth.loan.FineCalculator;
import s21_capstone.cap03_library_with_auth.loan.LibraryRules;
import s21_capstone.cap03_library_with_auth.loan.Loan;
import s21_capstone.cap03_library_with_auth.user.LibraryUser;
import s21_capstone.cap03_library_with_auth.user.Role;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy JEDNOSTKOWE domeny biblioteki — bez Springa. Kara to czysta funkcja (termin, dzień), więc sprawdzamy granice
 * bez zegara i bez bazy; zasady (0.50 zł/dzień) podajemy rekordem LibraryRules jak w konfiguracji.
 */
class Cap03LibraryWithAuthUnitTest {

    final FineCalculator fines = new FineCalculator(new LibraryRules(14, new BigDecimal("0.50"), 3));

    @ParameterizedTest(name = "termin {0}, dzień {1} → {2} dni, kara {3}")
    @CsvSource({
            "2026-10-15, 2026-10-14, 0, 0.00",     // przed terminem
            "2026-10-15, 2026-10-15, 0, 0.00",     // w dniu terminu — jeszcze bez kary
            "2026-10-15, 2026-10-16, 1, 0.50",
            "2026-10-15, 2026-10-19, 4, 2.00",     // Jan, Czysty kod
            "2026-09-15, 2026-10-19, 34, 17.00"})  // Maria, Wzorce projektowe
    @DisplayName("3. Kara = dni po terminie × 0.50 zł; w dniu terminu i wcześniej 0")
    void fineCalculator(LocalDate dueOn, LocalDate day, long days, BigDecimal fine) {
        assertThat(fines.daysOverdue(dueOn, day)).isEqualTo(days);
        assertThat(fines.fineFor(dueOn, day)).isEqualTo(fine);
    }

    @Test
    @DisplayName("3. Egzemplarz nie wyjdzie dwa razy; zwrot zakończonego wypożyczenia → 409 ALREADY_RETURNED")
    void bookAndLoanRules() {
        Book book = new Book("KSI-009", "Czysta architektura", "Robert C. Martin");
        book.markLoaned();
        assertThatThrownBy(book::markLoaned)
                .isInstanceOfSatisfying(ConflictException.class, e -> assertThat(e.code()).isEqualTo("BOOK_ON_LOAN"));

        Loan loan = new Loan(book, new LibraryUser("jan", "{bcrypt}...", "Jan Kowalski", Role.CZYTELNIK),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));
        loan.markReturned(LocalDate.of(2026, 10, 19), new BigDecimal("2.00"));
        assertThat(loan.isActive()).isFalse();
        assertThatThrownBy(() -> loan.markReturned(LocalDate.of(2026, 10, 20), BigDecimal.ZERO))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("ALREADY_RETURNED"));
        assertThat(loan.getFine()).as("kara z pierwszego zwrotu").isEqualByComparingTo("2.00");
    }
}
