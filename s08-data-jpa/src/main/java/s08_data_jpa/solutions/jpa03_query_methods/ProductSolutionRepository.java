package s08_data_jpa.solutions.jpa03_query_methods;

import org.springframework.data.jpa.repository.JpaRepository;
import s08_data_jpa.jpa03_query_methods.Product;
import s08_data_jpa.jpa03_query_methods.ProductCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Jpa03QueryMethods — zajrzyj dopiero po własnej próbie!
 * Pakiet solutions nie jest skanowany przez Jpa03Application, więc test tworzy to repozytorium fabryką
 * JpaRepositoryFactory (tak samo jak repozytorium ćwiczeń).
 */
public interface ProductSolutionRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryOrderByNameAsc(ProductCategory category);

    List<Product> findByPriceBetweenAndActiveTrueOrderByPriceAsc(BigDecimal min, BigDecimal max);

    Optional<Product> findFirstByCategoryAndActiveTrueOrderByPriceAsc(ProductCategory category);

    default List<Product> solution1(ProductCategory category) {
        return findByCategoryOrderByNameAsc(category);
    }

    default List<Product> solution2(BigDecimal min, BigDecimal max) {
        return findByPriceBetweenAndActiveTrueOrderByPriceAsc(min, max);
    }

    default Optional<Product> solution3(ProductCategory category) {
        return findFirstByCategoryAndActiveTrueOrderByPriceAsc(category);
    }
}
