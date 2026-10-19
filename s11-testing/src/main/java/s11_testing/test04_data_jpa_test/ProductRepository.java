package s11_testing.test04_data_jpa_test;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium produktów lekcji Test04DataJpaTest — to je testujemy wycinkiem {@code @DataJpaTest}.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryOrderBySku(String category);

    /** Produkty, których na stanie jest MNIEJ niż limit — od najmniejszego stanu, przy remisie po SKU. */
    @Query("select p from Product p where p.stock < :limit order by p.stock, p.sku")
    List<Product> findLowStock(int limit);
}
