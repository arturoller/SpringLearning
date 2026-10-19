package s05_validation.val04_custom_validators;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * CategoryCatalog = słownik kategorii sklepu — zwykły bean Springa. Walidator {@link KnownCategoryValidator}
 * dostaje go przez konstruktor (wstrzykiwanie zależności), co działa tylko wtedy, gdy walidatorem zarządza Spring.
 * Kategorie jak w JavaLearning (SampleData).
 */
@Component
public class CategoryCatalog {

    private final Set<String> categories = Set.of("ELEKTRONIKA", "SPOZYWCZE", "KSIAZKI", "ODZIEZ", "DOM");

    public boolean contains(String category) {
        return categories.contains(category);
    }
}
