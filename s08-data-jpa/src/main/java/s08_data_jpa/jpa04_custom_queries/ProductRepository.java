package s08_data_jpa.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * ProductRepository — zapytania pisane RĘCZNIE adnotacją {@code @Query} (query = zapytanie): JPQL, natywny SQL,
 * zapytania modyfikujące ({@code @Modifying}) i projekcje. Opis w lekcji Jpa04CustomQueries.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- 1. JPQL z parametrami nazwanymi -------------------------------------------------------------

    @Query("select p from Product p where p.price between :min and :max order by p.price")
    List<Product> findInPriceRange(@Param("min") BigDecimal min, @Param("max") BigDecimal max);

    /** Bez {@code @Param}: nazwa parametru z kodu bajtowego (kompilacja z -parameters, ustawione w starter-parent). */
    @Query("select p from Product p where p.category = :category and p.stock > 0 order by p.name")
    List<Product> findAvailableInCategory(String category);

    // --- 2. Natywny SQL ------------------------------------------------------------------------------

    @Query(value = "select * from products where stock = 0 order by name", nativeQuery = true)
    List<Product> findOutOfStockNative();

    // --- 3. @Modifying -------------------------------------------------------------------------------

    /** Masowa zmiana cen BEZ czyszczenia kontekstu utrwalania — PUŁAPKA z sekcji 3. */
    @Transactional
    @Modifying
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int changePrices(String category, BigDecimal factor);

    /** To samo z clearAutomatically = true (wyczyść automatycznie kontekst po zapytaniu). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int changePricesAndClear(String category, BigDecimal factor);

    /** Masowe usuwanie jednym zapytaniem DELETE (bez wczytywania encji — w przeciwieństwie do deleteBy). */
    @Transactional
    @Modifying
    @Query("delete from Product p where p.stock = 0")
    int deleteOutOfStock();

    // --- 4. Projekcje --------------------------------------------------------------------------------

    /** Projekcja interfejsowa z zapytania z nazwy metody — SQL pobiera tylko name i price. */
    List<ProductNameAndPrice> findByCategoryOrderByName(String category);

    /** Projekcja-rekord z zapytania z nazwy metody. */
    List<ProductSummary> findByStockGreaterThanOrderByName(int minStock);

    /** Klasyczne „select new” — pełna nazwa klasy w JPQL. */
    @Query("select new s08_data_jpa.jpa04_custom_queries.ProductSummary(p.name, p.price) "
            + "from Product p where p.category = :category order by p.name")
    List<ProductSummary> findSummariesSelectNew(String category);

    /** Rekord z aliasami w JPQL — bez „select new”. */
    @Query("select p.name as name, p.price as price from Product p where p.category = :category order by p.name")
    List<ProductSummary> findSummariesByAliases(String category);

    /**
     * „select p” (cała encja), a typ zwracany to rekord — Spring Data sam przepisuje zapytanie na pobranie tylko
     * kolumn potrzebnych rekordowi (w logu: select p1_0.name,p1_0.price ...). Nowość Spring Data JPA 3.5+.
     */
    @Query("select p from Product p where p.stock > 0 order by p.name")
    List<ProductSummary> findAvailableSummaries();

    /** Natywne zapytanie z grupowaniem → projekcja interfejsowa po aliasach kolumn. */
    @Query(value = "select category as category, sum(stock) as totalStock from products "
            + "group by category order by category", nativeQuery = true)
    List<CategoryStock> sumStockPerCategory();
}
