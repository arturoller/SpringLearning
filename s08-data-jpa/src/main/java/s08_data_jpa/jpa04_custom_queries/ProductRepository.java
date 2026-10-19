package s08_data_jpa.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repozytorium z zapytaniami pisanymi RĘCZNIE: {@code @Query} (JPQL albo natywne SQL), projekcje i {@code @Modifying}.
 * Gdy metoda ma {@code @Query}, jej nazwa nie ma znaczenia — Spring Data jej nie rozbiera (Jpa03QueryMethods).
 * Prawdziwy SQL metod — w komentarzach sekcji lekcji Jpa04CustomQueries.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Pomocnicza metoda z nazwy (Jpa03QueryMethods) — używana w testach lekcji. */
    Optional<Product> findBySku(String sku);

    // --- sekcja 1: JPQL (Jakarta Persistence Query Language — język zapytań o ENCJE i ich POLA) ---

    /** Parametry nazwane {@code :category}, {@code :maxPrice}; {@code @Param} zbędne, bo kompilujemy z -parameters. */
    @Query("select p from Product p where p.category = :category and p.price <= :maxPrice order by p.price")
    List<Product> findInCategoryUpTo(Category category, BigDecimal maxPrice);

    /** To samo z jawnym {@code @Param} (param = parametr) — działa zawsze, także bez flagi kompilatora -parameters. */
    @Query("select p from Product p where p.category = :category order by p.price desc")
    List<Product> findMostExpensiveFirst(@Param("category") Category category);

    /** Parametr pozycyjny {@code ?1} (pierwszy parametr metody) — spotkasz w starszym kodzie. */
    @Query("select p from Product p where p.stock < ?1 order by p.stock, p.sku")
    List<Product> findLowStock(int threshold);

    // --- sekcja 2: natywne SQL (nazwy TABEL i KOLUMN, składnia konkretnej bazy) ---

    /** {@code @NativeQuery} = nowszy skrót dla {@code @Query(value = ..., nativeQuery = true)} (w starych tutorialach go nie ma). */
    @NativeQuery("select * from products where stock = 0 order by sku")
    List<Product> findSoldOut();

    /** Starszy zapis tego samego. W natywnym SQL nie ma enumów Javy — kategorię przekazujemy jako tekst. */
    @Query(value = "select * from products where category = :category order by price desc limit :limit",
            nativeQuery = true)
    List<Product> findMostExpensiveNative(String category, int limit);

    // --- sekcja 3: projekcje interfejsowe ---

    /** Zapytanie z nazwy metody + projekcja: Spring Data pobiera tylko kolumny name i price. */
    List<ProductSummary> findByCategoryOrderByPriceAsc(Category category);

    /** Agregacja: aliasy (as productCount) muszą pasować do getterów projekcji CategoryStats. */
    @Query("""
            select p.category as category, count(p) as productCount, sum(p.price * p.stock) as stockValue
            from Product p group by p.category order by p.category""")
    List<CategoryStats> findCategoryStats();

    // --- sekcja 4: rekord DTO ---

    /** Zapytanie z nazwy metody zwracające rekord. */
    List<ProductPrice> findByCategoryOrderByPriceDesc(Category category);

    /** JPQL BEZ wyrażenia new: Spring Data 2026.0 sam przepisuje zapytanie na kolumny konstruktora rekordu. */
    @Query("select p from Product p where p.category = :category order by p.price")
    List<ProductPrice> findPrices(Category category);

    /** Klasyczny zapis z tutoriali: wyrażenie konstruktora {@code select new pełna.nazwa.Klasy(...)}. */
    @Query("select new s08_data_jpa.jpa04_custom_queries.ProductPrice(p.name, p.price) "
            + "from Product p where p.category = :category order by p.price")
    List<ProductPrice> findPricesWithNew(Category category);

    /** Natywne SQL też potrafi wypełnić rekord — kolumny w kolejności składowych rekordu. */
    @NativeQuery("select name, price from products where category = :category order by price")
    List<ProductPrice> findPricesNative(String category);

    // --- sekcje 5 i 6: zapytania zmieniające dane (modifying = modyfikujący;
    //     @Transactional = transakcyjny — metoda wykona się w transakcji) ---

    /** Zbiorcza zmiana cen: jedno UPDATE na wiele wierszy. Zwraca liczbę zmienionych wierszy. */
    @Transactional
    @Modifying
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int changePrices(BigDecimal factor, Category category);

    /**
     * Jak wyżej + {@code flushAutomatically} (przed UPDATE wypchnij oczekujące zmiany) i {@code clearAutomatically}
     * (po UPDATE wyczyść kontekst utrwalania — koniec z nieaktualnymi encjami).
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int changePricesAndClear(BigDecimal factor, Category category);

    /** Zbiorcze usuwanie produktów bez stanu. */
    @Transactional
    @Modifying
    @Query("delete from Product p where p.stock = 0")
    int deleteSoldOut();
}
