package s10_transactions.tx05_programmatic_transactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Tx05ProgrammaticTransactions — „WYNIK” każdej sekcji.
 *
 * <p>Jak w całym dziale: {@code @SpringBootTest} BEZ transakcji testu (żeby widzieć prawdziwe commity i rollbacki),
 * stan bazy czytany JdbcClientem, dane przywracane przed każdym testem. Ceny porównujemy przez
 * {@code isEqualByComparingTo} — BigDecimal 89.0 i 89.00 to dla equals RÓŻNE liczby (inna skala).</p>
 */
@SpringBootTest
@ActiveProfiles("tx05")
class Tx05ProgrammaticTransactionsTest {

    @Autowired
    PriceService priceService;

    @Autowired
    CheckoutService checkoutService;

    @Autowired
    PaymentGateway paymentGateway;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    TransactionTemplate sharedTemplate;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void resetCatalog() {
        jdbc.sql("update products set price = 5499.99 where sku = 'ELE-001'").update();
        jdbc.sql("update products set price = 349.90 where sku = 'ELE-003'").update();
        jdbc.sql("update products set price = 64.99 where sku = 'SPO-001'").update();
        jdbc.sql("update products set price = 79.00 where sku = 'KSI-001'").update();
        jdbc.sql("update products set price = 129.00 where sku = 'KSI-002'").update();
        jdbc.sql("delete from price_changes").update();
        jdbc.sql("delete from orders").update();
    }

    BigDecimal price(String sku) {
        return jdbc.sql("select price from products where sku = ?").param(sku).query(BigDecimal.class).single();
    }

    int count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
    }

    @Test
    @DisplayName("1. execute zwraca wynik lambdy i zatwierdza; executeWithoutResult — to samo bez wyniku")
    void executeAndExecuteWithoutResult() {
        assertThat(priceService.changePrice("KSI-001", new BigDecimal("89.00"))).as("stara cena")
                .isEqualByComparingTo("79.00");
        assertThat(price("KSI-001")).isEqualByComparingTo("89.00");

        priceService.changePriceWithoutResult("KSI-002", new BigDecimal("119.00"));
        assertThat(price("KSI-002")).isEqualByComparingTo("119.00");
        assertThat(count("price_changes")).as("dwa wpisy w dzienniku").isEqualTo(2);
    }

    @Test
    @DisplayName("1. Wyjątek w lambdzie albo przy commicie → rollback CAŁOŚCI, także wpisu w dzienniku")
    void exceptionRollsBackEverything() {
        assertThatThrownBy(() -> priceService.changePrice("SPO-001", new BigDecimal("0.00")))
                .as("CHECK (price > 0) łamany dopiero przy flush, czyli przy commicie")
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(price("SPO-001")).isEqualByComparingTo("64.99");
        assertThat(count("price_changes")).as("INSERT do dziennika też wycofany").isZero();

        assertThatThrownBy(() -> priceService.changePrice("XXX-999", new BigDecimal("10.00")))
                .isInstanceOf(NoSuchElementException.class);

        // Lambda TransactionCallback nie może rzucić wyjątku kontrolowanego: doInTransaction nie ma „throws”.
        assertThat(TransactionCallback.class.getMethods()[0].getExceptionTypes()).isEmpty();
    }

    @Test
    @DisplayName("2. setRollbackOnly: symulacja widzi swoją zmianę, ale nic nie zostaje zapisane")
    void setRollbackOnlyForDryRun() {
        assertThat(priceService.previewCatalogValue("SPO-001", new BigDecimal("74.99")))
                .as("suma cennika z kawą droższą o 10 zł").isEqualByComparingTo("6132.88");
        assertThat(price("SPO-001")).as("nic nie zapisane").isEqualByComparingTo("64.99");
    }

    @Test
    @DisplayName("3. Transakcja na element: zła cena i nieznany sku wycofane, reszta zapisana")
    void transactionPerItem() {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        prices.put("ELE-001", new BigDecimal("5299.99"));
        prices.put("SPO-001", new BigDecimal("0.00"));          // łamie CHECK (price > 0)
        prices.put("XXX-999", new BigDecimal("10.00"));         // nie ma takiego produktu
        prices.put("KSI-001", new BigDecimal("69.00"));

        PriceService.ImportReport report = priceService.importPrices(prices);

        assertThat(report.imported()).containsExactly("ELE-001", "KSI-001");
        assertThat(report.failed()).containsExactly(
                Map.entry("SPO-001", "DataIntegrityViolationException"),
                Map.entry("XXX-999", "NoSuchElementException"));
        assertThat(price("ELE-001")).isEqualByComparingTo("5299.99");
        assertThat(price("SPO-001")).isEqualByComparingTo("64.99");
        assertThat(price("KSI-001")).isEqualByComparingTo("69.00");
        assertThat(count("price_changes")).as("tylko dwa udane wpisy").isEqualTo(2);
    }

    @Test
    @DisplayName("3. Dla porównania jedna transakcja na cały import: jedna zła cena wycofuje wszystko")
    void oneTransactionForEverything() {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        prices.put("ELE-003", new BigDecimal("299.90"));
        prices.put("SPO-001", new BigDecimal("0.00"));

        assertThatThrownBy(() -> priceService.importPricesAllOrNothing(prices))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(price("ELE-003")).as("dobra cena też wycofana").isEqualByComparingTo("349.90");
    }

    @Test
    @DisplayName("4. Płatność POZA transakcją: bramka wołana bez transakcji, zapis w krótkiej transakcji")
    void externalCallOutsideTransaction() {
        checkoutService.checkoutInsideTransaction("Jan Kowalski", new BigDecimal("349.90"));
        assertThat(paymentGateway.wasTransactionActiveDuringLastCharge()).as("@Transactional na całości").isTrue();

        checkoutService.checkoutWithShortTransaction("Maria Nowak", new BigDecimal("64.99"));
        assertThat(paymentGateway.wasTransactionActiveDuringLastCharge()).as("TransactionTemplate tylko na zapis")
                .isFalse();

        assertThat(count("orders")).isEqualTo(2);
    }

    @Test
    @DisplayName("5. Własny szablon: nazwa, readOnly, REQUIRES_NEW; wspólny bean Boota zostaje bez zmian")
    void ownTemplateSettings() {
        assertThat(priceService.insideOwnReadOnlyTemplate())
                .isEqualTo(new PriceService.TemplateInsight("raport-cen", true, true));

        assertThat(context.getBeanNamesForType(TransactionTemplate.class)).containsExactly("transactionTemplate");
        assertThat(sharedTemplate.isReadOnly()).isFalse();
        assertThat(sharedTemplate.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
        assertThat(sharedTemplate.getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
    }

    @Test
    @DisplayName("6. Ręcznie: commit poza try daje prawdziwą przyczynę; commit w try → IllegalTransactionStateException")
    void manualTransactionManager() {
        assertThat(priceService.changePriceManually("KSI-001", new BigDecimal("75.00"))).isEqualByComparingTo("79.00");
        assertThat(price("KSI-001")).isEqualByComparingTo("75.00");

        assertThatThrownBy(() -> priceService.changePriceManually("SPO-001", new BigDecimal("0.00")))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> priceService.changePriceNaively("SPO-001", new BigDecimal("0.00")))
                .as("drugi rollback zasłania błąd CHECK")
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessage("Transaction is already completed - do not call commit or rollback more than once "
                        + "per transaction");
        assertThat(price("SPO-001")).isEqualByComparingTo("64.99");
    }

    @Test
    @DisplayName("6. PUŁAPKA: niezakończona transakcja zostaje przy wątku — kolejny kod do niej dołącza")
    void forgottenTransactionStaysBoundToThread() {
        TransactionStatus forgotten = priceService.beginAndForget();
        try {
            assertThat(forgotten.isNewTransaction()).isTrue();
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .as("metoda się skończyła, a transakcja trwa").isTrue();

            priceService.changePrice("KSI-001", new BigDecimal("99.00"));     // „zatwierdzona”? — dołączyła!
            assertThat(forgotten.isCompleted()).isFalse();
        } finally {
            transactionManager.rollback(forgotten);                            // sprzątanie (zawsze)
        }
        assertThat(price("KSI-001")).as("zmiana zniknęła razem z zapomnianą transakcją").isEqualByComparingTo("79.00");
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
    }

    @Test
    @DisplayName("7. Zapowiedź s17_events: @TransactionalEventListener domyślnie działa PO commicie")
    void transactionalEventListenerDefaultPhase() throws Exception {
        assertThat(TransactionalEventListener.class.getMethod("phase").getDefaultValue())
                .isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
