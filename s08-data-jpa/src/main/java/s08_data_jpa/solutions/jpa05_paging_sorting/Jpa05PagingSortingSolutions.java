package s08_data_jpa.solutions.jpa05_paging_sorting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import s08_data_jpa.jpa05_paging_sorting.Category;
import s08_data_jpa.jpa05_paging_sorting.Product;
import s08_data_jpa.jpa05_paging_sorting.ProductRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Jpa05PagingSorting. Używają ProductRepository Z LEKCJI, więc ich test
 * uruchamia kontekst lekcji (Jpa05Application).
 */
public final class Jpa05PagingSortingSolutions {

    private Jpa05PagingSortingSolutions() {
    }

    /** Rozwiązanie 1: druga strona = numer 1 (liczymy od zera). Nazwy są unikalne, więc jeden klucz wystarcza. */
    public static Page<Product> solution1(ProductRepository repository) {
        return repository.findAll(PageRequest.of(1, 4, Sort.by("name")));
    }

    /** Rozwiązanie 2: pętla po wycinkach; nextPageable() niesie numer strony, rozmiar i sortowanie. */
    public static List<String> solution2(ProductRepository repository, int pageSize) {
        List<String> skus = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("sku"));
        Slice<Product> slice;
        do {
            slice = repository.findByStockGreaterThan(0, pageable);
            slice.forEach(product -> skus.add(product.getSku()));
            pageable = slice.nextPageable();
        } while (slice.hasNext());
        return skus;
    }

    /** Rozwiązanie 3: cena malejąco, a remisy rozstrzyga unikalne sku. */
    public static Page<Product> solution3(ProductRepository repository) {
        return repository.findAll(PageRequest.of(1, 7, Sort.by("price").descending().and(Sort.by("sku"))));
    }

    /** Rozwiązanie 4: Page.map zamienia zawartość, zachowując numer strony i liczby. */
    public static Page<String> solution4(ProductRepository repository, Category category, int page, int size) {
        return repository.findByCategory(category, PageRequest.of(page, size, Sort.by("price").and(Sort.by("sku"))))
                .map(Product::getName);
    }
}
