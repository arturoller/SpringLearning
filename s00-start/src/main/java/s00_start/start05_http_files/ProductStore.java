package s00_start.start05_http_files;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
 * ProductStore = magazyn produktów w pamięci. {@code @Component} (komponent) — zwykły bean Springa.
 * Na start ma trzy produkty. Dane znikają po zatrzymaniu aplikacji (prawdziwa baza — od działu s07_data_jdbc).
 * Metody są synchronized (synchronizowane), bo serwer obsługuje zapytania równolegle, w wielu wątkach
 * (t21_concurrency/Concurrency02RaceConditions).
 */
@Component
public class ProductStore {

    private final TreeMap<Long, Product> products = new TreeMap<>();   // TreeMap = posortowane po id
    private long nextId = 1;

    public ProductStore() {
        add(new NewProduct("Kawa", 2999));
        add(new NewProduct("Herbata", 1599));
        add(new NewProduct("Czekolada", 899));
    }

    public synchronized List<Product> findAll() {
        return new ArrayList<>(products.values());   // kopia — nikt z zewnątrz nie zmieni naszej mapy
    }

    public synchronized Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    public synchronized Product add(NewProduct newProduct) {
        Product product = new Product(nextId++, newProduct.name(), newProduct.priceGrosze());
        products.put(product.id(), product);
        return product;
    }
}
