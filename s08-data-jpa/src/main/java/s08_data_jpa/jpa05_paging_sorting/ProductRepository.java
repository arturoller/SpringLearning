package s08_data_jpa.jpa05_paging_sorting;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repozytorium ze stronicowaniem i sortowaniem. {@code findAll(Pageable)} i {@code findAll(Sort)} są już w
 * JpaRepository (z ListPagingAndSortingRepository); poniżej — te same parametry w zapytaniach z nazw metod i w @Query.
 * Specjalne parametry (Pageable = opis żądanej strony, Sort = sortowanie, Limit) Spring Data rozpoznaje po TYPIE
 * i nie traktuje ich jak warunków.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Page = strona + łączna liczba wyników (dodatkowe zapytanie count). */
    Page<Product> findByCategory(Category category, Pageable pageable);

    /** Slice = „wycinek”: strona + informacja, czy jest następna — BEZ zapytania count. */
    Slice<Product> findByStockGreaterThan(int stock, Pageable pageable);

    /** Samo sortowanie, przekazywane z zewnątrz (bez OrderBy w nazwie metody). */
    List<Product> findByPriceLessThan(BigDecimal price, Sort sort);

    /** Limit (limit = ograniczenie liczby wyników) — przekazywany jako parametr zamiast Top3 w nazwie. */
    List<Product> findByCategoryOrderByPriceDesc(Category category, Limit limit);

    /** @Query + Pageable: zapytanie liczące Spring Data wyprowadzi sam z JPQL (select count(p) ...). */
    @Query("select p from Product p where p.price >= :minPrice")
    Page<Product> findPricedFrom(BigDecimal minPrice, Pageable pageable);
}
