package s18_aop.aop01_aspects;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Katalog produktów — zwykły serwis z logiką biznesową. NIE wie nic o aspektach: logowanie, sprawdzanie i liczenie
 * dokładają do niego aspekty z zewnątrz. Zapis „METODA …” w dzienniku jest tylko po to, żeby testy widziały,
 * W KTÓRYM MOMENCIE (między poradami) wykonało się ciało metody.
 */
@Service
public class ProductCatalog {

    private static final Map<String, Product> PRODUCTS = Stream.of(
                    new Product("ELE-001", "Laptop Pro 14", "ELEKTRONIKA", new BigDecimal("5499.99"), 7),
                    new Product("ELE-002", "Smartfon X", "ELEKTRONIKA", new BigDecimal("2999.00"), 0),
                    new Product("SPO-001", "Kawa ziarnista 1kg", "SPOZYWCZE", new BigDecimal("64.99"), 120),
                    new Product("SPO-002", "Czekolada gorzka", "SPOZYWCZE", new BigDecimal("7.49"), 300),
                    new Product("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00"), 15))
            .collect(Collectors.toUnmodifiableMap(Product::sku, Function.identity()));

    private final CallJournal journal;

    public ProductCatalog(CallJournal journal) {
        this.journal = journal;
    }

    public BigDecimal findPrice(String sku) {
        journal.add("METODA findPrice(" + sku + ")");
        return product(sku).price();
    }

    public int findStock(String sku) {
        journal.add("METODA findStock(" + sku + ")");
        return product(sku).stock();
    }

    public String findName(String sku) {
        return product(sku).name();
    }

    /** SKU produktów tańszych niż limit, posortowane (Map nie gwarantuje kolejności). */
    public List<String> findCheaperThan(BigDecimal limit) {
        return PRODUCTS.values().stream()
                .filter(product -> product.price().compareTo(limit) < 0)
                .map(Product::sku)
                .sorted()
                .toList();
    }

    /**
     * Wartość magazynu (cena x stan) — „tylko dla kierownika”. Adnotacja {@code @RolesAllowed} (dozwolone role,
     * z biblioteki Jakarta Annotations) jest tu tylko ZNACZNIKIEM dla aspektu z lekcji: sama niczego nie sprawdza.
     */
    @RolesAllowed("KIEROWNIK")
    public BigDecimal stockValue() {
        return PRODUCTS.values().stream()
                .map(product -> product.price().multiply(BigDecimal.valueOf(product.stock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Product product(String sku) {
        Product product = PRODUCTS.get(sku);
        if (product == null) {
            throw new IllegalArgumentException("Nieznany SKU: " + sku);
        }
        return product;
    }
}
