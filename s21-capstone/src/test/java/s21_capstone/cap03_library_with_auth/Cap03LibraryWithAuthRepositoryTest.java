package s21_capstone.cap03_library_with_auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap03_library_with_auth.book.Book;
import s21_capstone.cap03_library_with_auth.book.BookRepository;
import s21_capstone.cap03_library_with_auth.loan.Loan;
import s21_capstone.cap03_library_with_auth.loan.LoanRepository;
import s21_capstone.cap03_library_with_auth.user.LibraryUserRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wycinek bazy biblioteki ({@code @DataJpaTest}, baza wbudowana o losowej nazwie + migracje cap03).
 * Test {@code @Version} wyłącza transakcję testu ({@code Propagation.NOT_SUPPORTED}), żeby każde wywołanie repozytorium
 * było OSOBNĄ transakcją — jak dwóch bibliotekarzy przy dwóch komputerach.
 */
@DataJpaTest
@ActiveProfiles("cap03")
class Cap03LibraryWithAuthRepositoryTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    @Autowired
    LoanRepository loans;

    @Autowired
    BookRepository books;

    @Autowired
    LibraryUserRepository users;

    @Test
    @DisplayName("3. Zaległości i limit: Jan ma książkę po terminie, Adam nie; aktywne wypożyczenia liczone bez zwróconych")
    void overdueAndActiveCounts() {
        assertThat(loans.hasOverdue(users.findByUsername("jan").orElseThrow(), TODAY)).isTrue();
        assertThat(loans.hasOverdue(users.findByUsername("adam").orElseThrow(), TODAY)).isFalse();
        assertThat(loans.countActive(users.findByUsername("jan").orElseThrow())).isEqualTo(2);
        assertThat(loans.countActive(users.findByUsername("adam").orElseThrow())).as("zwrócona się nie liczy").isZero();
        assertThat(loans.hasOverdue(users.findByUsername("jan").orElseThrow(), LocalDate.of(2026, 10, 15)))
                .as("w dniu terminu to jeszcze nie zaległość").isFalse();
    }

    @Test
    @DisplayName("4. Wypożyczenia czytelnika z książką i czytelnikiem (join fetch), najnowsze pierwsze")
    void loansOfReader() {
        assertThat(loans.findAllOfReader("jan"))
                .extracting(loan -> loan.getBook().getCallNumber())
                .containsExactly("KSI-002", "KSI-001");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("2. @Version: dwóch bibliotekarzy wypożycza ten sam egzemplarz — drugi zapis odrzucony (wersja się zmieniła)")
    void optimisticLockingOnBook() {
        Book first = books.findById(6L).orElseThrow();    // bibliotekarz 1 czyta: version 0, na półce
        Book second = books.findById(6L).orElseThrow();   // bibliotekarz 2 czyta TO SAMO: version 0

        first.markLoaned();
        books.save(first);                                 // update books ... where id=? and version=? → version 1
        second.markLoaned();                               // w JEGO kopii książka wciąż „na półce”

        // save() kopii odłączonej = merge: Hibernate czyta wiersz (version 1), widzi, że kopia ma 0, i odrzuca zapis
        // jeszcze PRZED wysłaniem UPDATE (log SQL: select, potem wyjątek). W serwisie (encja zarządzana w transakcji)
        // ten sam konflikt wychodzi przy commicie: UPDATE ... where version = 0 nie trafia w żaden wiersz.
        assertThatThrownBy(() -> books.save(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Book fresh = books.findById(6L).orElseThrow();
        assertThat(fresh.isOnLoan()).isTrue();
        assertThat(fresh.getVersion()).isEqualTo(1);
        fresh.markReturned();                              // sprzątamy: baza tej klasy jest wspólna dla jej testów
        books.save(fresh);
    }

    @Test
    @DisplayName("3. Nowe wypożyczenie zapisuje się z terminem; Loan nie zna kary, dopóki książka nie wróci")
    void newLoan() {
        Loan loan = loans.save(new Loan(books.findById(5L).orElseThrow(), users.findByUsername("adam").orElseThrow(),
                TODAY, TODAY.plusDays(14)));
        assertThat(loan.getId()).isNotNull();
        assertThat(loan.getDueOn()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(loan.getFine()).isNull();
    }
}
