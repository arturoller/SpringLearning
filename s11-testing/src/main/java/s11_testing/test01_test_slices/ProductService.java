package s11_testing.test01_test_slices;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;

/**
 * Serwis produktów — logika sklepu. Nie zna HTTP. Czas pobiera z wstrzykniętego {@link Clock}, a dane z
 * repozytorium — dlatego da się go przetestować zwykłym JUnit + Mockito, BEZ uruchamiania Springa.
 */
@Service // service = serwis, warstwa logiki
public class ProductService {

    private final ProductRepository repository;
    private final Clock clock;

    // wstrzykiwanie przez konstruktor (constructor injection) — dzięki niemu test podstawia atrapy bez Springa
    public ProductService(ProductRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product get(long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    /** Tworzy produkt z datą utworzenia wziętą z zegara (a nie z {@code Instant.now()}). */
    public Product create(String name, BigDecimal price) {
        return repository.save(new Product(name, price, clock.instant()));
    }

    /**
     * Cena po rabacie o {@code percent} procent, zaokrąglona do groszy (HALF_UP).
     *
     * @throws IllegalArgumentException gdy rabat jest spoza 0..100
     * @throws ProductNotFoundException gdy nie ma produktu
     */
    public BigDecimal priceAfterDiscount(long id, int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Rabat musi być w przedziale 0..100, a jest " + percent);
        }
        BigDecimal price = get(id).getPrice();
        BigDecimal factor = BigDecimal.valueOf(100 - percent).divide(BigDecimal.valueOf(100));
        return price.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
