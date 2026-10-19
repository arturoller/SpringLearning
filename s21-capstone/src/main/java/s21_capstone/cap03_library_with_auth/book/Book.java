package s21_capstone.cap03_library_with_auth.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import s21_capstone.cap03_library_with_auth.common.ConflictException;

import java.util.Map;

/**
 * Egzemplarz książki (jeden wiersz = jeden egzemplarz na półce), z sygnaturą (call number), np. KSI-001.
 *
 * <p>{@code onLoan} + {@code @Version} (wersja — blokowanie optymistyczne): wypożyczenie ZMIENIA ten wiersz
 * (onLoan = true). Gdy dwóch bibliotekarzy wypożycza ten sam egzemplarz jednocześnie, obaj zmieniają TEN SAM wiersz —
 * drugi UPDATE ... where version = ? nie trafi w żaden wiersz i Hibernate zgłosi konflikt
 * (ObjectOptimisticLockingFailureException → porada → 409; Cap03LibraryWithAuthRepositoryTest pokazuje wariant z kopią
 * odłączoną). To przypadek, w którym {@code @Version} działa — w przeciwieństwie do dwóch NOWYCH rezerwacji
 * z Cap02Reservations.</p>
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "call_number", nullable = false, unique = true, length = 20)
    private String callNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(name = "on_loan", nullable = false)
    private boolean onLoan;

    @Version
    private long version;

    protected Book() {
    }

    public Book(String callNumber, String title, String author) {
        this.callNumber = callNumber;
        this.title = title;
        this.author = author;
    }

    /** Egzemplarz wychodzi z biblioteki. Drugi raz — konflikt (ktoś już go ma). */
    public void markLoaned() {
        if (onLoan) {
            throw new ConflictException("BOOK_ON_LOAN", "Egzemplarz " + callNumber + " jest już wypożyczony",
                    Map.of("callNumber", callNumber));
        }
        onLoan = true;
    }

    public void markReturned() {
        onLoan = false;
    }

    public Long getId() {
        return id;
    }

    public String getCallNumber() {
        return callNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isOnLoan() {
        return onLoan;
    }

    public long getVersion() {
        return version;
    }
}
