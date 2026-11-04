package s08_data_jpa.jpa03_query_methods;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Repozytorium z zapytaniami wyprowadzanymi z NAZW metod (derived queries — zapytania pochodne). Żadna metoda nie
 * ma implementacji ani SQL: Spring Data przy starcie rozbiera nazwę na części (PartTree — drzewo części) i buduje
 * z nich zapytanie JPA. Pod każdą metodą: JPQL, który z niej powstaje (w uproszczeniu).
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ---- 1. Słowa kluczowe w nazwach ---------------------------------------------------------------

    /** {@code where p.name = ?1} — nazwa jest unikalna, więc Optional (0 albo 1 wynik). */
    Optional<Product> findByName(String name);

    /** {@code where p.category = ?1 and p.price < ?2} */
    List<Product> findByCategoryAndPriceLessThan(Category category, BigDecimal maxPrice);

    /** {@code where upper(p.name) like upper('%' || ?1 || '%')} — Containing = zawiera, IgnoreCase = bez wielkości liter */
    List<Product> findByNameContainingIgnoreCase(String fragment);

    /** {@code where p.price between ?1 and ?2} — Between: OBA końce włącznie */
    List<Product> findByPriceBetweenOrderByPriceAscNameAsc(BigDecimal from, BigDecimal to);

    /** {@code where p.stock > ?1 order by p.price desc} — GreaterThan = większy niż, OrderBy…Desc = malejąco */
    List<Product> findByStockGreaterThanOrderByPriceDesc(int minStock);

    /** {@code select ... where p.name = ?1 fetch first 1 rows only} → true/false, bez wczytywania encji */
    boolean existsByName(String name);

    /** {@code select count(p) from Product p where p.category = ?1} */
    long countByCategory(Category category);

    /**
     * Usuwa produkty o danym stanie magazynu i zwraca ich liczbę. Pod spodem: SELECT pasujących encji, potem
     * {@code em.remove} dla KAŻDEJ (DELETE przy flush). Wymaga transakcji — stąd {@code @Transactional} (sekcja 1).
     */
    @Transactional
    long deleteByStock(int stock);

    /**
     * BŁĄD (do nauki): to samo co deleteByStock, ale BEZ {@code @Transactional}. Wywołane poza transakcją kończy się
     * wyjątkiem (test sekcji 1). removeBy… to synonim deleteBy….
     */
    long removeBySupplier(String supplier);

    /** Top3 = pierwsze trzy wyniki: {@code order by p.price desc fetch first 3 rows only} */
    List<Product> findTop3ByOrderByPriceDesc();

    /** First = pierwszy wynik (to samo co Top1): najtańszy produkt kategorii */
    Optional<Product> findFirstByCategoryOrderByPriceAsc(Category category);

    // ---- 2. Typy wyniku --------------------------------------------------------------------------------

    /** Pojedynczy obiekt: 0 wierszy → null, 1 → obiekt, 2+ → IncorrectResultSizeDataAccessException. */
    Product findOneByCategory(Category category);

    /** Optional przy kilku wynikach też rzuca IncorrectResultSizeDataAccessException — Optional to „0 albo 1”. */
    Optional<Product> findOptionalBySupplier(String supplier);

    /** Stream (strumień) — wiersze czytane kursorem w miarę przetwarzania; tylko w transakcji, trzeba go zamknąć. */
    Stream<Product> streamByCategory(Category category);

    // ---- 4. null jako parametr -------------------------------------------------------------------------

    /** {@code where p.supplier = ?1}, ale dla null Spring Data generuje {@code where p.supplier is null}. */
    List<Product> findBySupplierOrderByIdAsc(String supplier);

    /** Kategoria jest NOT NULL, więc findByCategory(null) → {@code is null} → zawsze pusta lista. */
    List<Product> findByCategory(Category category);
}
