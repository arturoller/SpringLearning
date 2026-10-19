package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * {@link ExitCodeGenerator} (generator kodu wyjścia): gdy katalog jest pusty (np. {@code --limit=0}), aplikacja
 * kończy się kodem 3 zamiast 0. {@code SpringApplication.exit(context)} pyta wszystkie takie beany o kod.
 * Kod wyjścia czytają skrypty i harmonogramy zadań (np. cron, Jenkins): 0 = sukces, inna liczba = problem.
 */
@Component
public class EmptyCatalogExitCode implements ExitCodeGenerator {

    /** Kod oznaczający „nic nie wczytano”. */
    public static final int EMPTY_CATALOG = 3;

    private final ProductCatalog catalog;

    public EmptyCatalogExitCode(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public int getExitCode() {
        return catalog.loaded().isEmpty() ? EMPTY_CATALOG : 0;
    }
}
