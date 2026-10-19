package s21_capstone.cap03_library_with_auth.bookreservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s21_capstone.cap03_library_with_auth.book.Book;

import java.util.Optional;

/** Repozytorium rezerwacji książek — gotowe pod ĆWICZENIE 3. */
public interface BookReservationRepository extends JpaRepository<BookReservation, Long> {

    /** Aktywna rezerwacja egzemplarza (razem z czytelnikiem), jeśli jest. */
    @Query("""
            select r from BookReservation r join fetch r.reader
            where r.book = :book and r.status = s21_capstone.cap03_library_with_auth.bookreservation.BookReservation.Status.AKTYWNA""")
    Optional<BookReservation> findActiveFor(@Param("book") Book book);
}
