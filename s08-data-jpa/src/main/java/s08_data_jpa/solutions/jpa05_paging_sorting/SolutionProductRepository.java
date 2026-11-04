package s08_data_jpa.solutions.jpa05_paging_sorting;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.JpaRepository;
import s08_data_jpa.jpa05_paging_sorting.Category;
import s08_data_jpa.jpa05_paging_sorting.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Jpa05PagingSorting (ExerciseProductRepository) — zajrzyj dopiero po własnej
 * próbie! Metody default mają te same nazwy co w ćwiczeniu.
 */
public interface SolutionProductRepository extends JpaRepository<Product, Long> {

    // Rozwiązanie 1
    Page<Product> findByCategory(Category category, Pageable pageable);

    default Page<Product> categoryPageByName(Category category, int page, int size) {
        return findByCategory(category, PageRequest.of(page, size, Sort.by("name")));
    }

    // Rozwiązanie 2: Slice — bez zapytania count; id jako ostatnie pole sortowania rozstrzyga remisy
    Slice<Product> findByStockGreaterThan(int minStock, Pageable pageable);

    default Slice<Product> availableCheapestFirst(int page, int size) {
        return findByStockGreaterThan(0, PageRequest.of(page, size, Sort.by("price").and(Sort.by("id"))));
    }

    // Rozwiązanie 3: Limit jako argument
    List<Product> findByCategoryOrderByPriceDescIdAsc(Category category, Limit limit);

    default List<Product> mostExpensive(Category category, int howMany) {
        return findByCategoryOrderByPriceDescIdAsc(category, Limit.of(howMany));
    }

    // Rozwiązanie 4: keyset — każde okno zaczyna się po kluczu (name, id) ostatniego elementu poprzedniego
    Window<Product> findFirst5ByOrderByNameAscIdAsc(ScrollPosition position);

    default List<String> allNamesInWindowsOfFive() {
        List<String> names = new ArrayList<>();
        Window<Product> window = findFirst5ByOrderByNameAscIdAsc(ScrollPosition.keyset());
        while (!window.isEmpty()) {
            window.forEach(product -> names.add(product.getName()));
            if (!window.hasNext()) {
                break;
            }
            window = findFirst5ByOrderByNameAscIdAsc(window.positionAt(window.size() - 1));
        }
        return names;
    }
}
