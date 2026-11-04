package s08_data_jpa.solutions.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import s08_data_jpa.jpa04_custom_queries.Category;
import s08_data_jpa.jpa04_custom_queries.Product;
import s08_data_jpa.jpa04_custom_queries.ProductPrice;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Jpa04CustomQueries (ExerciseProductRepository) — zajrzyj dopiero po własnej
 * próbie! Metody default mają te same nazwy co w ćwiczeniu, a pod spodem wywołują metody z {@code @Query}.
 */
public interface SolutionProductRepository extends JpaRepository<Product, Long> {

    // Rozwiązanie 1: lower(...) po obu stronach = porównanie bez wielkości liter
    @Query("select p from Product p where lower(p.name) like lower(concat('%', :fragment, '%')) order by p.name")
    List<Product> findByNameFragment(String fragment);

    default List<Product> searchByNameFragment(String fragment) {
        return findByNameFragment(fragment);
    }

    // Rozwiązanie 2: rekord bez "new" — Spring Data dopasowuje kolumny do konstruktora (sekcja 3)
    @Query("select p.name, p.price from Product p where p.category = :category order by p.price")
    List<ProductPrice> findPricesInCategory(Category category);

    default List<ProductPrice> pricesInCategory(Category category) {
        return findPricesInCategory(category);
    }

    // Rozwiązanie 3: coalesce zamienia null (brak wierszy) na 0
    @Query("select coalesce(sum(p.price * p.stock), 0) from Product p where p.category = :category")
    BigDecimal sumStockValue(Category category);

    default BigDecimal stockValue(Category category) {
        return sumStockValue(category);
    }

    // Rozwiązanie 4: clearAutomatically = true — po UPDATE kontekst jest pusty, findById czyta z bazy
    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.stock = :amount where p.stock = 0")
    int setStockWhereEmpty(int amount);

    default int restockEmpty(int amount) {
        return setStockWhereEmpty(amount);
    }
}
