package s21_capstone.cap03_library_with_auth.loan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s21_capstone.cap03_library_with_auth.user.LibraryUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Repozytorium wypożyczeń — zapytania z join fetch, bo odpowiedź zawsze potrzebuje książki i czytelnika. */
public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Query("select l from Loan l join fetch l.book join fetch l.reader where l.id = :id")
    Optional<Loan> findDetailedById(@Param("id") Long id);

    /** Wypożyczenia czytelnika: najpierw najnowsze. */
    @Query("""
            select l from Loan l join fetch l.book join fetch l.reader r
            where r.username = :username
            order by l.loanedOn desc, l.id desc""")
    List<Loan> findAllOfReader(@Param("username") String username);

    @Query("select count(l) from Loan l where l.reader = :reader and l.returnedOn is null")
    long countActive(@Param("reader") LibraryUser reader);

    /** Czy czytelnik ma niezwróconą książkę po terminie (termin przed {@code today}). */
    @Query("""
            select count(l) > 0 from Loan l
            where l.reader = :reader and l.returnedOn is null and l.dueOn < :today""")
    boolean hasOverdue(@Param("reader") LibraryUser reader, @Param("today") LocalDate today);
}
