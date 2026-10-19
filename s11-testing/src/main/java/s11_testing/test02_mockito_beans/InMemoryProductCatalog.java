package s11_testing.test02_mockito_beans;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Prawdziwy katalog produktów: kilka produktów sklepu w niezmiennej mapie. Bean Springa ({@code @Component}).
 */
@Component
public class InMemoryProductCatalog implements ProductCatalog {

    private final Map<String, Product> products = Map.of(
            "SPO-001", new Product("SPO-001", "Kawa ziarnista 1kg", 6499),
            "SPO-002", new Product("SPO-002", "Czekolada gorzka", 749),
            "KSI-001", new Product("KSI-001", "Czysty kod", 7900),
            "DOM-002", new Product("DOM-002", "Lampka biurkowa", 12900));

    @Override
    public Product getBySku(String sku) {
        Product product = products.get(sku);
        if (product == null) {
            throw new NoSuchElementException("Nieznany produkt: " + sku);
        }
        return product;
    }
}
