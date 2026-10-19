package s08_data_jpa.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PRZYKŁADY BŁĘDNE (do sekcji 2 i 5 lekcji Jpa04CustomQueries) — NIE kopiuj. Każda metoda się kompiluje,
 * a aplikacja STARTUJE — błąd wychodzi dopiero przy wywołaniu. Dlatego trzymamy je osobno od ProductRepository.
 * ({@code @Transactional} = transakcyjny: metoda wykona się w transakcji; {@code @Modifying} = modyfikujący.)
 */
public interface ProductMistakesRepository extends JpaRepository<Product, Long> {

    /** BŁĄD: UPDATE bez {@code @Modifying} — Spring Data wykona go jak SELECT (getResultList). */
    @Transactional
    @Query("update Product p set p.stock = 0 where p.category = :category")
    int clearStockWithoutModifying(Category category);

    /** BŁĄD: {@code @Modifying} bez transakcji — zadziała w teście (test ma transakcję), padnie w aplikacji. */
    @Modifying
    @Query("update Product p set p.stock = 0 where p.category = :category")
    int clearStockWithoutTransaction(Category category);

    /** BŁĄD: literówka w nazwie kolumny natywnego SQL (cena zamiast price) — natywnego SQL nikt nie sprawdza przy starcie. */
    @NativeQuery("select * from products where cena > 100")
    List<Product> findExpensiveWithTypo();
}
