package s05_validation.val04_custom_validators;

import org.springframework.stereotype.Component;

import java.util.Set;

/** CategoryCatalog = katalog kategorii sklepu (bean ziarno Springa, tu na stałe w pamięci; w życiu — baza danych). */
@Component
public class CategoryCatalog {

    private final Set<String> categories = Set.of("AGD", "RTV", "SPOZYWCZE");

    public boolean exists(String category) {
        return categories.contains(category);
    }
}
