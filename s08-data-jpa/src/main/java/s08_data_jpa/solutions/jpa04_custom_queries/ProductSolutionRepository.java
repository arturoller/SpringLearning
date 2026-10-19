package s08_data_jpa.solutions.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import s08_data_jpa.jpa04_custom_queries.Category;
import s08_data_jpa.jpa04_custom_queries.Product;
import s08_data_jpa.jpa04_custom_queries.StockValue;

import java.math.BigDecimal;
import java.util.List;

/** Rozwiązania ćwiczeń lekcji Jpa04CustomQueries — zapytania (zajrzyj po własnej próbie!). */
public interface ProductSolutionRepository extends JpaRepository<Product, Long> {

    /** Rozwiązanie 1: parametr nazwany i sortowanie po dwóch polach (drugie rozstrzyga remisy). */
    @Query("select p from Product p where p.stock < :threshold order by p.stock, p.sku")
    List<Product> findStockBelow(int threshold);

    /** Rozwiązanie 2: wyrażenie liczone w bazie; kolumny w kolejności składowych rekordu StockValue(sku, value). */
    @Query("select p.sku, p.price * p.stock from Product p where p.category = :category order by p.sku")
    List<StockValue> findStockValues(Category category);

    /** Rozwiązanie 3: natywne SQL — nazwy tabeli i kolumn, LIMIT jako parametr. */
    @NativeQuery("select * from products where category = :category order by price limit :limit")
    List<Product> findCheapestNative(String category, int limit);

    /** Rozwiązanie 4: transakcja + @Modifying z wypchnięciem zmian przed i wyczyszczeniem kontekstu po UPDATE. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int multiplyPrices(BigDecimal factor, Category category);
}
