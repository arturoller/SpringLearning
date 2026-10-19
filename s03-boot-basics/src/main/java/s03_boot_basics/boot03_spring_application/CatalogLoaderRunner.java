package s03_boot_basics.boot03_spring_application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runner nr 1: wczytuje katalog produktów przy starcie. {@link ApplicationRunner} (uruchamiacz aplikacji) dostaje
 * argumenty już rozłożone na części ({@link ApplicationArguments}):
 * <pre>
 *   --limit=3 plik.csv   →   opcja „limit” = [3],   argumenty bez opcji = [plik.csv]
 * </pre>
 * Brak {@code --limit} = cały katalog. {@code --limit=abc} kończy się wyjątkiem NumberFormatException
 * i aplikacja NIE wystartuje (lekcja, sekcja 5).
 */
@Component
@Order(1)
public class CatalogLoaderRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogLoaderRunner.class);

    private final ProductCatalog catalog;
    private final StartupJournal journal;

    public CatalogLoaderRunner(ProductCatalog catalog, StartupJournal journal) {
        this.catalog = catalog;
        this.journal = journal;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> limitValues = args.getOptionValues("limit");   // null, gdy opcji nie podano
        int limit = (limitValues == null || limitValues.isEmpty())
                ? ProductCatalog.ALL_PRODUCTS.size()
                : Integer.parseInt(limitValues.getFirst());
        catalog.load(limit);
        log.info("Wczytano produktów: {}", catalog.loaded().size());
        journal.record("runner 1 (ApplicationRunner): opcje " + args.getOptionNames()
                + ", bez opcji " + args.getNonOptionArgs() + ", wczytano " + catalog.loaded().size());
    }
}
