package s21_capstone.cap01_shop_api.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repozytorium produktów (Spring Data JPA — implementację tworzy Spring w czasie startu). */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /** Wiele produktów jednym zapytaniem ({@code where sku in (...)}) — przy składaniu zamówienia. */
    List<Product> findBySkuIn(List<String> skus);

    /**
     * Wyszukiwanie z OPCJONALNYMI filtrami: parametr {@code null} = „bez tego warunku”. Spring Data dopisuje do
     * zapytania sortowanie i stronicowanie z {@link Pageable}, a do {@code Page} — drugie zapytanie {@code count}.
     */
    @Query("""
            select p from Product p
            where (:category is null or p.category = :category)
              and (:text is null or lower(p.name) like lower(concat('%', :text, '%')))""")
    Page<Product> search(@Param("category") Category category, @Param("text") String text, Pageable pageable);
}
