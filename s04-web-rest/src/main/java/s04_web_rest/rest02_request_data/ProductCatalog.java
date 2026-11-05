package s04_web_rest.rest02_request_data;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Katalog w pamięci (bez bazy — baza dopiero od działu s07). Niezmienna lista pięciu produktów. */
@Component
public class ProductCatalog {

    private static final List<Product> PRODUCTS = List.of(
            new Product(1, "Kawa", "napoje", 2499, List.of("bio", "import")),
            new Product(2, "Herbata", "napoje", 1599, List.of("bio")),
            new Product(3, "Czekolada", "słodycze", 899, List.of("import")),
            new Product(4, "Ciastka", "słodycze", 1299, List.of()),
            new Product(5, "Kubek", "akcesoria", 3499, List.of("import")));

    public List<Product> all() {
        return PRODUCTS;
    }

    public Optional<Product> byId(long id) {
        return PRODUCTS.stream().filter(product -> product.id() == id).findFirst();
    }
}
