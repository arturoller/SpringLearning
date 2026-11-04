package s05_validation.val02_valid_in_controllers;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Serwis produktów — prosty, w pamięci. Jego rola w lekcji: w testach podmieniamy go atrapą (@MockitoBean) i
 * sprawdzamy {@code verifyNoInteractions} — dowód, że przy błędnych danych kontroler NIE doszedł do logiki.
 */
@Service
public class ProductService {

    public Product create(ProductRequest request) {
        return new Product(3, request.name(), request.price(), request.quantity());
    }

    public Optional<Product> find(long id) {
        return Optional.empty();
    }

    public List<Product> search(String query) {
        return List.of();
    }
}
