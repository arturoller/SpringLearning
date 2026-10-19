package s21_capstone.cap03_library_with_auth.bookreservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import s21_capstone.cap03_library_with_auth.book.Book;
import s21_capstone.cap03_library_with_auth.user.LibraryUser;

import java.time.LocalDate;

/**
 * Rezerwacja WYPOŻYCZONEGO egzemplarza (tabela z V3__book_reservations.sql) — infrastruktura pod ĆWICZENIE 3
 * projektu Cap03LibraryWithAuth. AKTYWNA = czeka na zwrot; ZREALIZOWANA = rezerwujący wypożyczył książkę.
 */
@Entity
@Table(name = "book_reservations")
public class BookReservation {

    /** Stan rezerwacji książki. */
    public enum Status {
        AKTYWNA, ZREALIZOWANA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id")
    private LibraryUser reader;

    @Column(name = "created_on", nullable = false)
    private LocalDate createdOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    protected BookReservation() {
    }

    public BookReservation(Book book, LibraryUser reader, LocalDate createdOn) {
        this.book = book;
        this.reader = reader;
        this.createdOn = createdOn;
        this.status = Status.AKTYWNA;
    }

    public void markFulfilled() {
        this.status = Status.ZREALIZOWANA;
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

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public Status getStatus() {
        return status;
    }
}
