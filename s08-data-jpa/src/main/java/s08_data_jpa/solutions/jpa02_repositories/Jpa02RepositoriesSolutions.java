package s08_data_jpa.solutions.jpa02_repositories;

import s08_data_jpa.jpa02_repositories.Product;
import s08_data_jpa.jpa02_repositories.ProductRepository;

import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń 1 i 2 lekcji Jpa02Repositories (ćwiczenie 3 — klasa GiftCard obok).
 * Metody przyjmują repozytorium Z LEKCJI, więc ich test uruchamia kontekst lekcji (Jpa02Application).
 */
public final class Jpa02RepositoriesSolutions {

    private Jpa02RepositoriesSolutions() {
    }

    /** Rozwiązanie 1: orElseThrow z własnym komunikatem; save zwraca zarządzany obiekt — na nim liczymy dalej. */
    public static int solution1(ProductRepository repository, Long id, int quantity) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brak produktu o id " + id));
        product.setStock(product.getStock() + quantity);
        return repository.save(product).getStock();
    }

    /** Rozwiązanie 2: filtr w Javie (zapytanie findByStock poznasz w Jpa03QueryMethods) i jedno deleteAll. */
    public static int solution2(ProductRepository repository) {
        List<Product> soldOut = repository.findAll().stream()
                .filter(product -> product.getStock() == 0)
                .toList();
        repository.deleteAll(soldOut);
        return soldOut.size();
    }
}
