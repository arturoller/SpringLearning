package s08_data_jpa.solutions.jpa02_repositories;

import s08_data_jpa.jpa02_repositories.Product;
import s08_data_jpa.jpa02_repositories.ProductNotFoundException;
import s08_data_jpa.jpa02_repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jpa02Repositories — zajrzyj dopiero po własnej próbie! */
public final class Jpa02RepositoriesSolutions {

    private Jpa02RepositoriesSolutions() {
    }

    /** Rozwiązanie 1: Optional.map — puste pudełko zostaje puste, pełne zamienia się na cenę. */
    public static Optional<BigDecimal> solution1PriceOf(ProductRepository repository, long id) {
        return repository.findById(id).map(Product::getPrice);
    }

    /** Rozwiązanie 2: deleteById nie zgłasza braku, więc sprawdzamy istnienie sami. */
    public static boolean solution2DeleteIfExists(ProductRepository repository, long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    /** Rozwiązanie 3: saveAll zwraca listę zapisanych encji w kolejności argumentu. */
    public static List<Long> solution3AddAll(ProductRepository repository, List<Product> products) {
        return repository.saveAll(products).stream().map(Product::getId).toList();
    }

    /** Rozwiązanie 4: NOWY obiekt (id == null → persist); wczytany oryginał zostaje nietknięty. */
    public static Product solution4Copy(ProductRepository repository, long id, String newName) {
        Product original = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        Product copy = new Product(newName, original.getCategory(), original.getPrice(), original.getStock(),
                original.getCreatedAt());
        return repository.save(copy);
    }
}
