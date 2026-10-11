package s04_web_rest.rest03_responses;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * ProductStore = magazyn produktów w pamięci (jak w s00_start/Start05HttpFiles). Metody synchronized, bo serwer
 * obsługuje zapytania w wielu wątkach naraz. Na start: Kawa (1), Herbata (2), Czekolada (3).
 */
@Component
public class ProductStore {

    private final TreeMap<Long, Product> products = new TreeMap<>();
    private long nextId = 1;

    public ProductStore() {
        add(new NewProduct("Kawa", 2999));
        add(new NewProduct("Herbata", 1599));
        add(new NewProduct("Czekolada", 899));
    }

    public synchronized List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public synchronized Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public synchronized Product add(NewProduct newProduct) {
        Product product = new Product(nextId++, newProduct.name(), newProduct.priceGrosze());
        products.put(product.id(), product);
        return product;
    }

    /** @return true, gdy produkt był i został usunięty; false, gdy go nie było */
    public synchronized boolean remove(long id) {
        return products.remove(id) != null;
    }
}
