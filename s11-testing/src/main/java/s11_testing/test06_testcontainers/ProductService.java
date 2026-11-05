package s11_testing.test06_testcontainers;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Serwis produktów lekcji Test06Testcontainers — cienka warstwa nad repozytorium. */
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getName().compareTo(b.getName())) // kolejność ustalona w kodzie, nie „jak baza da”
                .toList();
    }

    /** {@code true}, gdy produkt został dodany; {@code false}, gdy produkt o tej nazwie już był. */
    @Transactional // zapytanie @Modifying wymaga transakcji
    public boolean addIfAbsent(String name, BigDecimal price) {
        return repository.insertIfAbsent(name, price) == 1;
    }

    @Transactional(readOnly = true)
    public List<Product> mostExpensive(int n) {
        return repository.findMostExpensive(n);
    }
}
