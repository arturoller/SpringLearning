package s05_validation.val05_service_validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProductService = serwis produktów z walidacją metod. {@code @Validated} NA KLASIE sprawia, że Spring owija bean w proxy
 * (obiekt-pośrednik), które przed każdym wywołaniem sprawdza ograniczenia parametrów, a po — ograniczenia wyniku.
 * Naruszenie = {@code jakarta.validation.ConstraintViolationException}, a metoda się nie wykonuje.
 */
@Service
@Validated
public class ProductService {

    public static final String SKU_FORMAT = "[A-Z]{3}-[0-9]{3}";

    private final Map<String, Product> products = new ConcurrentHashMap<>();
    /** Stany magazynowe; BAD-001 to „uszkodzone dane” (ujemny stan) — do pokazania walidacji wyniku. */
    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of("ELE-001", 7, "ELE-002", 0, "BAD-001", -3));
    private final AtomicInteger added = new AtomicInteger();

    /**
     * Ograniczenia parametru-obiektu: {@code @NotNull} (jest podany) i {@code @Valid} (kaskada do jego pól — Val03).
     * Metoda wykonuje się tylko dla poprawnego polecenia.
     */
    public Product add(@NotNull @Valid NewProduct command) {
        Product product = new Product(command.sku(), command.name(), command.price());
        products.put(product.sku(), product);
        added.incrementAndGet();
        return product;
    }

    /** Ograniczenie parametru prostego i ograniczenie WYNIKU ({@code @PositiveOrZero} przy typie zwracanym). */
    @PositiveOrZero
    public int stockOf(@Pattern(regexp = SKU_FORMAT) String sku) {
        return stock.getOrDefault(sku, 0);
    }

    /** Ograniczenie na parametrze metody publicznej — zadziała, gdy wywołasz ją PRZEZ proxy (z innego beana). */
    public String normalize(@NotBlank String sku) {
        return sku.strip().toUpperCase();
    }

    /**
     * Wywołuje {@code normalize} przez {@code this} — czyli OMIJA proxy. Ograniczenie {@code @NotBlank} z {@code normalize}
     * nie zadziała (pułapka: wywołanie z tej samej klasy). Sama {@code describe} nie ma ograniczeń.
     */
    public String describe(String sku) {
        return "SKU: " + this.normalize(sku);
    }

    /** Ile razy wykonało się {@code add} (do sprawdzenia, że przy błędzie walidacji ciało metody się NIE wykonało). */
    public int addedCount() {
        return added.get();
    }

    public Product find(String sku) {
        return products.get(sku);
    }
}
