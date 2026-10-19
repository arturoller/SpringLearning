package s08_data_jpa.solutions.jpa03_query_methods;

import org.springframework.data.jpa.repository.JpaRepository;
import s08_data_jpa.jpa03_query_methods.Category;
import s08_data_jpa.jpa03_query_methods.Product;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/** Rozwiązania ćwiczeń lekcji Jpa03QueryMethods — zapytania z nazw metod (zajrzyj po własnej próbie!). */
public interface ProductSolutionRepository extends JpaRepository<Product, Long> {

    /** Rozwiązanie 1: dwa warunki (równość, mniejszy niż) + sortowanie rosnąco. */
    List<Product> findByCategoryAndPriceLessThanOrderByPriceAsc(Category category, BigDecimal price);

    /** Rozwiązanie 2: count…By — liczy baza; Containing dokłada %…%, IgnoreCase porównuje wielkimi literami. */
    long countByNameContainingIgnoreCase(String fragment);

    /** Rozwiązanie 3: Top2 + warunki + sortowanie po cenie rosnąco = dwa najtańsze. */
    List<Product> findTop2ByCategoryAndStockGreaterThanOrderByPriceAsc(Category category, int stock);

    /** Rozwiązanie 4: In (kolekcja), Between (dwa parametry: od, do), OrderBy po nazwie. */
    List<Product> findByCategoryInAndPriceBetweenOrderByNameAsc(Collection<Category> categories,
                                                                BigDecimal min, BigDecimal max);
}
