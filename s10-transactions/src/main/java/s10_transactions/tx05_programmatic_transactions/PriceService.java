package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PriceService = zmiany cen w cenniku. Prawie wszystko tu robimy BEZ {@code @Transactional}: transakcje otwieramy
 * w kodzie — przez TransactionTemplate (szablon transakcji) albo wprost przez PlatformTransactionManager.
 */
@Service
public class PriceService {

    private final ProductRepository products;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactionTemplate;          // bean z Boota — WSPÓLNY dla całej aplikacji
    private final PlatformTransactionManager transactionManager;

    public PriceService(ProductRepository products, JdbcClient jdbc, TransactionTemplate transactionTemplate,
                        PlatformTransactionManager transactionManager) {
        this.products = products;
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.transactionManager = transactionManager;
    }

    /** Zmiana ceny + wpis w dzienniku zmian — oba kroki w BIEŻĄCEJ transakcji (metoda nie otwiera własnej). */
    private BigDecimal changeInCurrentTransaction(String sku, BigDecimal newPrice) {
        Product product = products.findById(sku).orElseThrow();
        BigDecimal oldPrice = product.getPrice();
        product.changePrice(newPrice);                                // dirty checking → UPDATE przy commicie
        jdbc.sql("insert into price_changes (sku, old_price, new_price) values (?, ?, ?)")
                .params(sku, oldPrice, newPrice)
                .update();                                            // INSERT od razu (w tej samej transakcji)
        return oldPrice;
    }

    // --- 1. execute / executeWithoutResult -----------------------------------------------------------------------

    /** execute: transakcja wokół lambdy, wynik lambdy wraca do wołającego (tu: stara cena). */
    public BigDecimal changePrice(String sku, BigDecimal newPrice) {
        return transactionTemplate.execute(status -> changeInCurrentTransaction(sku, newPrice));
    }

    /** executeWithoutResult: to samo, gdy nie potrzebujesz wyniku (lambda dostaje status, nic nie zwraca). */
    public void changePriceWithoutResult(String sku, BigDecimal newPrice) {
        transactionTemplate.executeWithoutResult(status -> changeInCurrentTransaction(sku, newPrice));
    }

    // --- 2. setRollbackOnly — „symulacja” --------------------------------------------------------------------------

    /**
     * Jaka byłaby wartość całego cennika po zmianie ceny? Zmieniamy cenę NAPRAWDĘ (w transakcji), pytamy bazę o sumę,
     * a na końcu oznaczamy transakcję „tylko do wycofania” — nic nie zostaje zapisane.
     */
    public BigDecimal previewCatalogValue(String sku, BigDecimal newPrice) {
        return transactionTemplate.execute(status -> {
            products.findById(sku).orElseThrow().changePrice(newPrice);
            products.flush();                                         // flush = wypchnij UPDATE teraz, żeby SQL go widział
            BigDecimal total = jdbc.sql("select sum(price) from products").query(BigDecimal.class).single();
            status.setRollbackOnly();                                 // to tylko symulacja → ROLLBACK zamiast COMMIT
            return total;
        });
    }

    // --- 3. Transakcja na element w pętli ------------------------------------------------------------------------

    /** Wynik importu: zaimportowane sku i nieudane (sku → nazwa wyjątku), w kolejności z wejścia. */
    public record ImportReport(List<String> imported, Map<String, String> failed) {
    }

    /**
     * Import cennika: KAŻDA cena we własnej transakcji. Zła cena wycofuje tylko swoją zmianę (i swój wpis w dzienniku),
     * a import idzie dalej. Wyjątek łapiemy POZA execute — bo błąd z bazy (CHECK) pojawia się dopiero przy commicie,
     * który TransactionTemplate robi na końcu execute.
     */
    public ImportReport importPrices(Map<String, BigDecimal> newPrices) {
        List<String> imported = new ArrayList<>();
        Map<String, String> failed = new LinkedHashMap<>();
        newPrices.forEach((sku, price) -> {
            try {
                transactionTemplate.executeWithoutResult(status -> changeInCurrentTransaction(sku, price));
                imported.add(sku);
            } catch (RuntimeException importFailed) {
                failed.put(sku, importFailed.getClass().getSimpleName());
            }
        });
        return new ImportReport(imported, failed);
    }

    /** Dla porównania: cały import w JEDNEJ transakcji (adnotacja) — jedna zła cena wycofuje wszystko. */
    @Transactional
    public void importPricesAllOrNothing(Map<String, BigDecimal> newPrices) {
        newPrices.forEach(this::changeInCurrentTransaction);
    }

    // --- 5. Własne ustawienia szablonu -----------------------------------------------------------------------------

    /** Co widać wewnątrz transakcji z WŁASNEGO szablonu: nazwa, readOnly, czy to nowa transakcja. */
    public record TemplateInsight(String name, boolean readOnly, boolean newTransaction) {
    }

    /**
     * Własny szablon: new TransactionTemplate(menedżer) + ustawienia. NIE zmieniamy wspólnego beana transactionTemplate
     * (zmiana dotknęłaby wszystkich, którzy go używają).
     */
    public TemplateInsight insideOwnReadOnlyTemplate() {
        TransactionTemplate reportTemplate = new TransactionTemplate(transactionManager);
        reportTemplate.setName("raport-cen");                                  // nazwa widoczna w logu
        reportTemplate.setReadOnly(true);
        reportTemplate.setTimeout(10);                                         // sekundy
        reportTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return reportTemplate.execute(status -> new TemplateInsight(
                TransactionSynchronizationManager.getCurrentTransactionName(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
                status.isNewTransaction()));
    }

    // --- 6. PlatformTransactionManager niskopoziomowo -----------------------------------------------------------

    /**
     * Ręcznie, POPRAWNIE: rollback tylko, gdy zawiedzie NASZ kod; commit POZA try. Jeśli nie uda się sam commit
     * (np. CHECK w bazie przy flush), menedżer sam wycofuje transakcję i rzuca wyjątek z prawdziwą przyczyną.
     */
    public BigDecimal changePriceManually(String sku, BigDecimal newPrice) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();   // REQUIRED, DEFAULT…
        definition.setName("zmiana-ceny-recznie");                                     // nazwa do logu
        TransactionStatus status = transactionManager.getTransaction(definition);
        BigDecimal oldPrice;
        try {
            oldPrice = changeInCurrentTransaction(sku, newPrice);
        } catch (RuntimeException | Error e) {
            transactionManager.rollback(status);
            throw e;
        }
        transactionManager.commit(status);
        return oldPrice;
    }

    /**
     * ZNAJDŹ BŁĄD (sekcja 6): commit W ŚRODKU try. Gdy commit się nie uda, menedżer już wycofał i ZAKOŃCZYŁ
     * transakcję, a catch woła rollback drugi raz → IllegalTransactionStateException, która ZASŁANIA prawdziwą
     * przyczynę (błąd CHECK z bazy).
     */
    public BigDecimal changePriceNaively(String sku, BigDecimal newPrice) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            BigDecimal oldPrice = changeInCurrentTransaction(sku, newPrice);
            transactionManager.commit(status);
            return oldPrice;
        } catch (RuntimeException e) {
            transactionManager.rollback(status);
            throw e;
        }
    }

    /**
     * PUŁAPKA: rozpoczęta i NIEZAKOŃCZONA transakcja (tylko do pokazania!). Zostaje przypięta do wątku razem
     * z połączeniem z puli — każdy następny kod transakcyjny w tym wątku po cichu do niej DOŁĄCZY.
     */
    public TransactionStatus beginAndForget() {
        return transactionManager.getTransaction(new DefaultTransactionDefinition());
    }
}
