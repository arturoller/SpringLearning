package s08_data_jpa.solutions.jpa03_query_methods;

import org.springframework.data.jpa.repository.JpaRepository;
import s08_data_jpa.jpa03_query_methods.Category;
import s08_data_jpa.jpa03_query_methods.Product;

import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Jpa03QueryMethods (ExerciseProductRepository) — zajrzyj dopiero po własnej
 * próbie! Metody default mają te same nazwy co w ćwiczeniu, a pod spodem wywołują metody zapytań z nazw.
 */
public interface SolutionProductRepository extends JpaRepository<Product, Long> {

    // Rozwiązanie 1
    List<Product> findBySupplierOrderByNameAsc(String supplier);

    default List<Product> bySupplierSortedByName(String supplier) {
        return findBySupplierOrderByNameAsc(supplier);
    }

    // Rozwiązanie 2: zero jako argument — nazwa nie może zawierać liczby jako wartości warunku
    List<Product> findByCategoryAndStockGreaterThanOrderByPriceAsc(Category category, int minStock);

    default List<Product> availableInCategoryCheapestFirst(Category category) {
        return findByCategoryAndStockGreaterThanOrderByPriceAsc(category, 0);
    }

    // Rozwiązanie 3
    long countByNameStartingWithIgnoreCase(String prefix);

    default long countNameStartingWithIgnoreCase(String prefix) {
        return countByNameStartingWithIgnoreCase(prefix);
    }

    // Rozwiązanie 4: IsNull nie potrzebuje parametru
    List<Product> findTop2BySupplierIsNullOrderByPriceAsc();

    default List<Product> twoCheapestWithoutSupplier() {
        return findTop2BySupplierIsNullOrderByPriceAsc();
    }
}
