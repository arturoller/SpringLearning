package s21_capstone.cap03_library_with_auth.loan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import s21_capstone.cap03_library_with_auth.book.Book;
import s21_capstone.cap03_library_with_auth.common.ConflictException;
import s21_capstone.cap03_library_with_auth.user.LibraryUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Wypożyczenie egzemplarza przez czytelnika: od kiedy, do kiedy, kiedy zwrócony i z jaką karą. */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id")
    private LibraryUser reader;

    @Column(name = "loaned_on", nullable = false)
    private LocalDate loanedOn;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Column(name = "returned_on")
    private LocalDate returnedOn;

    /** Kara naliczona przy zwrocie (null, dopóki książka nie wróciła). */
    @Column(precision = 8, scale = 2)
    private BigDecimal fine;

    /** Pod ĆWICZENIE 1: czy termin już raz przedłużono. */
    @Column(nullable = false)
    private boolean extended;

    protected Loan() {
    }

    public Loan(Book book, LibraryUser reader, LocalDate loanedOn, LocalDate dueOn) {
        this.book = book;
        this.reader = reader;
        this.loanedOn = loanedOn;
        this.dueOn = dueOn;
    }

    public boolean isActive() {
        return returnedOn == null;
    }

    /** Zwrot: zapisujemy dzień i karę; drugi zwrot tego samego wypożyczenia to konflikt. */
    public void markReturned(LocalDate day, BigDecimal fine) {
        if (!isActive()) {
            throw new ConflictException("ALREADY_RETURNED", "To wypożyczenie zostało już zakończone " + returnedOn,
                    Map.of("returnedOn", returnedOn.toString()));
        }
        this.returnedOn = day;
        this.fine = fine;
    }

    /** Pod ĆWICZENIE 1: przesuwa termin o {@code days} dni i zapamiętuje, że przedłużono. Reguły sprawdza wołający. */
    public void extendBy(int days) {
        this.dueOn = dueOn.plusDays(days);
        this.extended = true;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public LibraryUser getReader() {
        return reader;
    }

    public LocalDate getLoanedOn() {
        return loanedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public boolean isExtended() {
        return extended;
    }
}
