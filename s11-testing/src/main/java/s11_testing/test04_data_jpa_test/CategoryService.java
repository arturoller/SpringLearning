package s11_testing.test04_data_jpa_test;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serwis z błędem do nauki: {@link #productNamesOf(long)} czyta LENIWĄ kolekcję kategorii bez transakcji. W aplikacji
 * to {@code LazyInitializationException}; w {@code @DataJpaTest} (test w jednej transakcji) błąd jest UKRYTY.
 */
@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    /** BŁĄD: transakcja repozytorium kończy się po findById, a kolekcja products jest czytana już po niej. */
    public List<String> productNamesOf(long categoryId) {
        Category category = repository.findById(categoryId).orElseThrow();
        return category.getProducts().stream().map(Product::getName).sorted().toList();
    }

    /** POPRAWNIE: jedna transakcja obejmuje odczyt kategorii i jej leniwej kolekcji. */
    @Transactional(readOnly = true) // read only = tylko do odczytu
    public List<String> productNamesOfInTransaction(long categoryId) {
        return productNamesOf(categoryId);
    }
}
