package s08_data_jpa.jpa05_paging_sorting;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repozytorium lekcji Jpa05PagingSorting. Ostatni parametr typu {@code Pageable} (stronicowanie), {@code Sort}
 * (sortowanie), {@code Limit} (limit) albo {@code ScrollPosition} (pozycja przewijania) Spring Data rozpoznaje
 * po TYPIE — nie jest on warunkiem zapytania, tylko dokłada ORDER BY / OFFSET / FETCH FIRST.
 * Sam {@code JpaRepository} ma też gotowe {@code findAll(Pageable)} i {@code findAll(Sort)}.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ---- 1–2. Page, Slice, List z Pageable ------------------------------------------------------------

    /** Page (strona) = treść + liczba wszystkich wyników → DODATKOWE zapytanie count. */
    Page<Product> findByCategory(Category category, Pageable pageable);

    /** Slice (wycinek) = treść + „czy jest następny?” → bez count; pobiera size + 1 wierszy. */
    Slice<Product> findByStockGreaterThan(int minStock, Pageable pageable);

    /** List z Pageable = sama treść strony: ani count, ani informacji o następnej stronie. */
    List<Product> findByPriceLessThan(BigDecimal maxPrice, Pageable pageable);

    // ---- 3. Sort -------------------------------------------------------------------------------------

    /** Sortowanie wybiera WYWOŁUJĄCY (np. z parametru ?sort= w REST), a nie nazwa metody. */
    List<Product> findByCategory(Category category, Sort sort);

    // ---- 4. @Query + Pageable, Limit, Top ------------------------------------------------------------

    /** @Query + Pageable: zapytanie count Spring Data wyprowadza SAM z zapytania głównego. */
    @Query("select p from Product p where p.price >= :minPrice")
    Page<Product> findPricedFrom(BigDecimal minPrice, Pageable pageable);

    /** Własne countQuery — gdy automatyczne byłoby złe albo wolne (np. przy złączeniach, s09). */
    @Query(value = "select p from Product p where p.price >= :minPrice",
            countQuery = "select count(p.id) from Product p where p.price >= :minPrice")
    Page<Product> findPricedFromWithCountQuery(BigDecimal minPrice, Pageable pageable);

    /** Limit (Spring Data 3.2+): liczba wyników jako ARGUMENT, a nie w nazwie metody (Top3). */
    List<Product> findByCategoryOrderByPriceAscIdAsc(Category category, Limit limit);

    /** Top10 w nazwie — liczba „zaszyta” na sztywno (Jpa03, sekcja 1). */
    List<Product> findTop10ByOrderByPriceDescIdAsc();

    // ---- 5. Keyset — ScrollPosition i Window --------------------------------------------------------

    /**
     * Przewijanie (scrolling): First4 = rozmiar okna, OrderBy = klucz sortowania (UNIKALNY dzięki id na końcu),
     * ScrollPosition = skąd zacząć. Window (okno) = wyniki + pozycja do następnego wywołania.
     */
    Window<Product> findFirst4ByOrderByPriceAscIdAsc(ScrollPosition position);
}
