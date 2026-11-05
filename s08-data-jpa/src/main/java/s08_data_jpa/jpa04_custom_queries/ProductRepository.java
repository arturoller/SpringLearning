package s08_data_jpa.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repozytorium lekcji Jpa04CustomQueries: zapytania pisane RĘCZNIE w {@code @Query} (query = zapytanie) — JPQL
 * albo natywny SQL — zamiast wyprowadzanych z nazw metod (Jpa03). Nazwa metody może wtedy znaczyć coś biznesowego.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ---- 1. @Query z JPQL ------------------------------------------------------------------------------

    /** Parametr nazwany {@code :minPrice} — bez {@code @Param}, bo kompilujemy z flagą -parameters (sekcja 1). */
    @Query("select p from Product p where p.price > :minPrice order by p.price desc")
    List<Product> findMoreExpensiveThan(BigDecimal minPrice);

    /** Parametry pozycyjne {@code ?1}, {@code ?2} — numer = kolejność parametru metody (od 1). */
    @Query("select p from Product p where p.category = ?1 and p.stock > ?2 order by p.name")
    List<Product> findAvailableInCategory(Category category, int minStock);

    /** Filtr OPCJONALNY: {@code category = null} wyłącza warunek (por. Jpa03, sekcja 4 — tam null dawał IS NULL). */
    @Query("""
            select p from Product p
            where (:category is null or p.category = :category)
              and p.price <= :maxPrice
            order by p.id""")
    List<Product> search(Category category, BigDecimal maxPrice);

    // ---- 2. Natywny SQL --------------------------------------------------------------------------------

    /** SQL bazy H2: funkcja REGEXP_LIKE (wyrażenie regularne). W PostgreSQL to byłby operator {@code ~}. */
    @Query(value = "SELECT * FROM product WHERE REGEXP_LIKE(name, :pattern) ORDER BY id", nativeQuery = true)
    List<Product> findByNameRegex(String pattern);

    /** Natywne zapytanie może zwrócić pojedynczą wartość: tu sumę stanów magazynowych jednej kategorii. */
    @Query(value = "SELECT COALESCE(SUM(stock), 0) FROM product WHERE category = :category", nativeQuery = true)
    long sumStockNative(String category);

    // ---- 3. Projekcje ----------------------------------------------------------------------------------

    /** Projekcja interfejsowa z metody z NAZWY: SELECT tylko name i price. */
    List<ProductSummary> findByCategoryOrderByPriceAsc(Category category);

    /** Projekcja interfejsowa w @Query: aliasy ({@code as name}) muszą pasować do getterów. */
    @Query("select p.name as name, p.price as price from Product p where p.stock = 0 order by p.name")
    List<ProductSummary> findOutOfStockSummaries();

    /** Projekcja DTO z wyrażeniem konstruktora: {@code select new pełna.nazwa.Klasy(…)} — klasyczny JPQL. */
    @Query("select new s08_data_jpa.jpa04_custom_queries.ProductPrice(p.name, p.price) "
            + "from Product p where p.stock = 0 order by p.name")
    List<ProductPrice> findOutOfStockPricesWithNew();

    /** Projekcja DTO BEZ {@code new}: Spring Data sam przepisuje zapytanie na wyrażenie konstruktora (sekcja 3). */
    @Query("select p.name, p.price from Product p where p.stock = 0 order by p.name")
    List<ProductPrice> findOutOfStockPrices();

    /** BŁĄD (do nauki): projekcja interfejsowa BEZ aliasów — gettery po cichu zwracają null (sekcja 3). */
    @Query("select p.name, p.price from Product p where p.stock = 0 order by p.name")
    List<ProductSummary> findOutOfStockSummariesWithoutAliases();

    /** BŁĄD (do nauki): kolumny w innej kolejności niż parametry konstruktora rekordu (sekcja 3). */
    @Query("select p.price, p.name from Product p where p.stock = 0 order by p.name")
    List<ProductPrice> findOutOfStockPricesWrongOrder();

    /** Projekcja OTWARTA (z @Value) — wygodna, ale wczytuje całe encje (PUŁAPKA sekcji 3). */
    List<ProductLabel> findLabelsByCategoryOrderByIdAsc(Category category);

    /** Projekcja DYNAMICZNA: typ wyniku wybiera wywołujący — {@code Product.class}, {@code ProductSummary.class}… */
    <T> List<T> findByCategoryOrderByIdAsc(Category category, Class<T> type);

    // ---- 4. @Modifying — UPDATE/DELETE jednym poleceniem -----------------------------------------------

    /** Masowa zmiana ceny BEZ czyszczenia kontekstu — encje w pamięci zostają ze starą ceną (PUŁAPKA sekcji 4). */
    @Modifying // modifying = modyfikujący
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int changePrices(Category category, BigDecimal factor);

    /** To samo, ale po UPDATE Spring Data czyści kontekst utrwalania (clearAutomatically = czyść automatycznie). */
    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int changePricesAndClear(Category category, BigDecimal factor);

    /** Najpierw flush zmian z pamięci (flushAutomatically = zapisz automatycznie), potem UPDATE, potem clear. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.price = p.price * :factor where p.category = :category")
    int changePricesFlushAndClear(Category category, BigDecimal factor);

    /** Masowy DELETE: JEDNO polecenie SQL (por. deleteBy… z Jpa03: SELECT + DELETE na każdą encję). */
    @Modifying
    @Query("delete from Product p where p.stock = 0")
    int deleteOutOfStock();

    /** BŁĄD (do nauki): UPDATE w @Query bez @Modifying — Spring Data wykona go jak SELECT (sekcja 4). */
    @Query("update Product p set p.stock = 0 where p.id = :id")
    int resetStockWithoutModifying(Long id);

    // ---- 5. Agregaty i SpEL ----------------------------------------------------------------------------

    /** Agregaty z group by → rekord przez {@code select new}. */
    @Query("select new s08_data_jpa.jpa04_custom_queries.CategoryStats(p.category, count(p), avg(p.price)) "
            + "from Product p group by p.category order by p.category")
    List<CategoryStats> categoryStats();

    /** Pojedyncza wartość agregatu. Brak wierszy → max(…) zwraca null, stąd typ obiektowy BigDecimal. */
    @Query("select max(p.price) from Product p where p.category = :category")
    BigDecimal maxPriceIn(Category category);

    /** SpEL {@code #{#entityName}} — nazwa encji repozytorium wstawiana przy starcie (przydatne w repozytoriach bazowych). */
    @Query("select p from #{#entityName} p where p.stock = 0 order by p.id")
    List<Product> findOutOfStock();
}
