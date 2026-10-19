package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Runner nr 2: {@link CommandLineRunner} (uruchamiacz wiersza poleceń) dostaje SUROWE argumenty — tablicę tekstów
 * dokładnie taką, jaką dostał {@code main}. Sam musisz je rozłożyć. {@code @Order(2)} — po CatalogLoaderRunner.
 */
@Component
@Order(2)
public class StartupReportRunner implements CommandLineRunner {

    private final ProductCatalog catalog;
    private final StartupJournal journal;

    public StartupReportRunner(ProductCatalog catalog, StartupJournal journal) {
        this.catalog = catalog;
        this.journal = journal;
    }

    @Override
    public void run(String... args) {
        journal.record("runner 2 (CommandLineRunner): argumenty " + Arrays.toString(args)
                + ", w katalogu " + catalog.loaded().size());
    }
}
