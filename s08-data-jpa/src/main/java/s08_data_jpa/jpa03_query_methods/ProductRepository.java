package s08_data_jpa.jpa03_query_methods;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Repozytorium z zapytaniami z NAZW METOD (derived queries — zapytania wyprowadzone z nazwy). Żadna metoda nie ma
 * ciała ani SQL: Spring Data przy starcie rozbiera nazwę na części (find…By, warunki, OrderBy) i buduje z niej
 * zapytanie. Prawdziwy SQL metod — w komentarzach sekcji lekcji Jpa03QueryMethods.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- sekcja 1: równość ---
    List<Product> findByCategory(Category category);

    Optional<Product> findBySku(String sku);

    // --- sekcja 2: operatory (less than = mniejszy niż; between = pomiędzy; containing = zawierający;
    //     ignore case = bez rozróżniania wielkości liter; starting with = zaczynający się od; in = należy do zbioru;
    //     greater than = większy niż; and = i; or = lub) ---
    List<Product> findByPriceLessThan(BigDecimal price);

    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    List<Product> findByNameContainingIgnoreCase(String fragment);

    List<Product> findByNameStartingWith(String prefix);

    List<Product> findByCategoryIn(Collection<Category> categories);

    List<Product> findByCategoryAndStockGreaterThan(Category category, int stock);

    List<Product> findByStockOrPriceGreaterThan(int stock, BigDecimal price);

    // --- sekcja 3: sortowanie i limity (order by = sortuj według; desc = malejąco; asc = rosnąco;
    //     top 3 = trzy pierwsze; first = pierwszy) ---
    List<Product> findByCategoryOrderByPriceDesc(Category category);

    List<Product> findTop3ByOrderByPriceDesc();

    Optional<Product> findFirstByCategoryOrderByPriceAsc(Category category);

    // --- sekcja 4: exists i count (exists = czy istnieje; count = policz) ---
    boolean existsBySku(String sku);

    long countByCategory(Category category);

    // --- sekcja 5: typy wyników ---
    Optional<Product> findOneByCategory(Category category);    // PUŁAPKA: kategoria ma kilka produktów → wyjątek

    Stream<Product> streamByCategory(Category category);       // stream = strumień; zamykaj w try-with-resources
}
