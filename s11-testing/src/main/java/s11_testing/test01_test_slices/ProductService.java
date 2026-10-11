package s11_testing.test01_test_slices;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * ProductService = serwis produktów. Warstwa pośrednia między kontrolerem a repozytorium.
 * W {@code @WebMvcTest} tego beana NIE MA (wycinek webowy nie skanuje {@code @Service}) — podajemy atrapę {@code @MockitoBean}.
 */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    /** Produkt po id; brak → NoSuchElementException (kontroler zamienia go na 404). */
    public ProductDto byId(long id) {
        return repository.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new NoSuchElementException("Brak produktu o id " + id));
    }

    /** Wyszukiwanie po fragmencie nazwy. */
    public List<ProductDto> search(String fragment) {
        return repository.findByNameContainingIgnoreCaseOrderByNameAsc(fragment).stream()
                .map(ProductDto::from)
                .toList();
    }
}
