package s04_web_rest.rest05_crud_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import s04_web_rest.rest05_crud_api.CrudModels.Product;
import s04_web_rest.rest05_crud_api.CrudModels.ProductRequest;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Rest05CrudApi — test JEDNOSTKOWY repozytorium (bez Springa): czy jest bezpieczne dla wielu wątków.
 * Wynik jest deterministyczny: niezależnie od przeplotu wątków ma powstać dokładnie 200 produktów o różnych id.
 */
class Rest05ProductRepositoryTest {

    @Test
    @DisplayName("5. 200 równoległych create(...) → 203 produkty (3 startowe + 200) i 200 różnych nowych id")
    void concurrentCreates() throws Exception {
        ProductRepository repository = new ProductRepository();

        List<Future<Product>> futures;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {   // wątki wirtualne (Java 21)
            futures = IntStream.range(0, 200)
                    .mapToObj(i -> executor.submit(() -> repository.create(new ProductRequest("P" + i, i))))
                    .toList();
        }   // close() czeka na zakończenie wszystkich zadań

        List<Long> ids = futures.stream().map(f -> {
            try {
                return f.get().id();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
        assertThat(ids).doesNotHaveDuplicates().hasSize(200);
        assertThat(repository.findAll()).hasSize(203);
        assertThat(repository.findAll()).extracting(Product::id).isSorted();   // lista posortowana po id
    }
}
