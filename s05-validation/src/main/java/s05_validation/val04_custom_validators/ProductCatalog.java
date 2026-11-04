package s05_validation.val04_custom_validators;

import org.springframework.stereotype.Component;

import java.util.Set;

/** Mały "katalog kategorii" sklepu — zwykły bean (ziarno) Springa, który wstrzykniemy do walidatora. */
@Component
public class ProductCatalog {

    public boolean categoryExists(String category) {
        return categories().contains(category);
    }

    public Set<String> categories() {
        return Set.of("kawa", "herbata", "akcesoria");
    }
}
