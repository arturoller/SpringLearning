package s07_data_jdbc.solutions.jdbc05_repository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import s07_data_jdbc.jdbc05_repository.Product;
import s07_data_jdbc.jdbc05_repository.ProductNotFoundException;
import s07_data_jdbc.jdbc05_repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.UnaryOperator;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jdbc05Repository — zajrzyj dopiero po własnej próbie! */
public final class Jdbc05RepositorySolutions {

    private Jdbc05RepositorySolutions() {
    }

    /** Rozwiązanie 1: findAll + filtr po cenie (compareTo, nie equals) + sortowanie alfabetyczne. */
    public static List<String> solution1NamesFromPrice(ProductRepository repository, BigDecimal minPrice) {
        return repository.findAll().stream()
                .filter(product -> product.price().compareTo(minPrice) >= 0)
                .map(Product::name)
                .sorted()
                .toList();
    }

    /** Rozwiązanie 2: najpierw podklasa (DuplicateKeyException), potem nadklasa (DataIntegrityViolationException). */
    public static String solution2TryInsert(ProductRepository repository, Product product) {
        try {
            repository.save(product);
            return "OK";
        } catch (DuplicateKeyException e) {
            return "DUPLICATE";
        } catch (DataIntegrityViolationException e) {
            return "INTEGRITY";
        }
    }

    /** Rozwiązanie 3: odczyt, kopia z nowym stanem, zapis; brak produktu to błąd biznesowy. */
    public static Product solution3Restock(ProductRepository repository, long id, int amount) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        return repository.save(product.withStock(product.stock() + amount));
    }

    /** Rozwiązanie 4: przy konflikcie wersji odczyt od nowa i kolejna próba; po trzeciej wyjątek leci dalej. */
    public static Product solution4UpdateWithRetry(ProductRepository repository, long id,
                                                    UnaryOperator<Product> change) {
        int maxAttempts = 3;
        for (int attempt = 1; ; attempt++) {
            Product fresh = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
            try {
                return repository.save(change.apply(fresh));
            } catch (OptimisticLockingFailureException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
            }
        }
    }
}
