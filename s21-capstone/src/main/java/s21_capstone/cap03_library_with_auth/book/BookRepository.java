package s21_capstone.cap03_library_with_auth.book;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repozytorium książek. */
public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByCallNumber(String callNumber);

    List<Book> findAllByOrderByCallNumber();
}
