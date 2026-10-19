package s20_modulith.mod02_module_events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import s20_modulith.mod02_module_events.inventory.InventoryManagement;
import s20_modulith.mod02_module_events.inventory.internal.InventoryEventHandler;
import s20_modulith.mod02_module_events.orders.OrderManagement;
import s20_modulith.mod02_module_events.orders.OrderPlaced;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Testy lekcji Mod02ModuleEvents — „wynik” sekcji 1–4.
 *
 * <p>Wybór rodzaju testu: {@code @SpringBootTest} z profilem {@code mod02} — potrzebujemy całej aplikacji (dwa moduły,
 * baza z migracjami Flyway, rejestr zdarzeń, pula wątków). Słuchacze są ASYNCHRONICZNI, więc na skutek czekamy
 * biblioteką Awaitility ({@code await().until(...)} — sprawdza warunek co chwilę, najdłużej 10 s), nigdy przez
 * {@code Thread.sleep}. Dane: każdy test ma własne numery zamówień i własne SKU, a stan magazynu sprawdza względnie
 * („o 2 mniej niż przed”) — kolejność testów nie ma znaczenia.</p>
 */
@SpringBootTest
@ActiveProfiles("mod02")
class Mod02ModuleEventsTest {

    /** Identyfikator słuchacza magazynu w rejestrze (kolumna LISTENER_ID) — prawdziwa wartość z bazy. */
    static final String INVENTORY_LISTENER_ID = "s20_modulith.mod02_module_events.inventory.internal."
            + "InventoryEventHandler.on(s20_modulith.mod02_module_events.orders.OrderPlaced)";

    /**
     * Sonda (probe) tylko na potrzeby testu: drugi słuchacz OrderPlaced, który zapisuje, w jakim wątku i w jakiej
     * transakcji go wywołano. Bean jest proxy (@Async, @Transactional) — dlatego dane czytamy przez METODĘ.
     */
    static class ListenerProbe {

        record Run(String thread, boolean transactionActive, String transactionName) {
        }

        private final Map<String, Run> runs = new ConcurrentHashMap<>();

        @ApplicationModuleListener
        public void on(OrderPlaced event) {
            runs.put(event.orderId(), new Run(Thread.currentThread().getName(),
                    TransactionSynchronizationManager.isActualTransactionActive(),
                    TransactionSynchronizationManager.getCurrentTransactionName()));
        }

        Run runFor(String orderId) {
            return runs.get(orderId);
        }
    }

    @TestConfiguration
    static class ProbeConfiguration {
        @Bean
        ListenerProbe listenerProbe() {
            return new ListenerProbe();
        }
    }

    @Autowired
    private OrderManagement orders;

    @Autowired
    private InventoryManagement inventory;

    @Autowired
    private ListenerProbe probe;

    @Autowired
    private InventoryEventHandler handler;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private IncompleteEventPublications incompletePublications;

    @Autowired
    private CompletedEventPublications completedPublications;

    @Test
    @DisplayName("1. Zdarzenie zamiast wywołania: orders nie zależy od inventory, a magazyn i tak reaguje na zamówienie")
    void section1() {
        ApplicationModules modules = ApplicationModules.of(Mod02Application.class);
        assertThat(dependencies(modules, "orders")).isEmpty();
        assertThat(dependencies(modules, "inventory")).containsExactly("orders");    // przez typ zdarzenia
        assertThat(modules.getModuleByName("inventory").orElseThrow().getEventsListenedTo(modules))
                .extracting(type -> type.getSimpleName()).containsExactly("OrderPlaced");
        assertThat(modules.detectViolations().getMessages()).isEmpty();

        int before = inventory.stockOf("KSI-001").orElseThrow();
        orders.placeOrder("ZAM-101", "KSI-001", 2);

        assertThat(orders.statusOf("ZAM-101")).contains("NOWE");
        await().until(() -> inventory.stockOf("KSI-001").orElseThrow() == before - 2);
    }

    @Test
    @DisplayName("2. @ApplicationModuleListener: inny wątek, po zatwierdzeniu transakcji, we własnej nowej transakcji")
    void section2() {
        orders.placeOrder("ZAM-201", "SPO-001", 3);
        await().until(() -> probe.runFor("ZAM-201") != null);

        ListenerProbe.Run run = probe.runFor("ZAM-201");
        assertThat(run.thread()).startsWith("task-").isNotEqualTo(Thread.currentThread().getName());
        assertThat(run.transactionActive()).isTrue();
        assertThat(run.transactionName()).isEqualTo(ListenerProbe.class.getName() + ".on");   // nowa transakcja słuchacza

        // bean słuchacza to proxy (asynchroniczność i transakcja dokładane przez AOP)
        assertThat(AopUtils.isAopProxy(handler)).isTrue();

        // wycofana transakcja publikującego: publikacje znikają razem z zamówieniem, słuchacze nie startują
        int stockBefore = inventory.stockOf("DOM-001").orElseThrow();
        transactionTemplate.executeWithoutResult(status -> {
            orders.placeOrder("ZAM-202", "DOM-001", 1);
            assertThat(publicationsOf("ZAM-202")).hasSize(2);       // w trakcie: po wierszu na słuchacza (magazyn + sonda)
            status.setRollbackOnly();
        });
        assertThat(publicationsOf("ZAM-202")).isEmpty();
        assertThat(orders.statusOf("ZAM-202")).isEmpty();
        assertThat(probe.runFor("ZAM-202")).isNull();
        assertThat(inventory.stockOf("DOM-001")).contains(stockBefore);
    }

    @Test
    @DisplayName("3. Rejestr publikacji: wiersz na każdego słuchacza, zdarzenie jako JSON, po sukcesie COMPLETED")
    void section3() {
        orders.placeOrder("ZAM-301", "ELE-003", 1);
        await().until(() -> publicationsOf("ZAM-301").stream().allMatch(row -> "COMPLETED".equals(row.get("STATUS")))
                && publicationsOf("ZAM-301").size() == 2);

        Map<String, Object> row = inventoryPublicationOf("ZAM-301");
        assertThat(row.get("EVENT_TYPE")).isEqualTo("s20_modulith.mod02_module_events.orders.OrderPlaced");
        assertThat(row.get("SERIALIZED_EVENT")).isEqualTo("{\"orderId\":\"ZAM-301\",\"sku\":\"ELE-003\",\"quantity\":1}");
        assertThat(row.get("COMPLETION_DATE")).isNotNull();
        assertThat(row.get("COMPLETION_ATTEMPTS")).isEqualTo(1);

        // to samo przez API Javy: CompletedEventPublications (tryb UPDATE — ukończone zostają w tabeli)
        assertThat(completedPublications.findAll())
                .anySatisfy(publication -> assertThat(publication.getEvent())
                        .isEqualTo(new OrderPlaced("ZAM-301", "ELE-003", 1)));
    }

    @Test
    @DisplayName("4. Błąd słuchacza zostawia publikację FAILED; po naprawie ponowienie ją kończy")
    void section4() {
        orders.placeOrder("ZAM-401", "NOW-001", 1);                 // magazyn nie zna NOW-001 → słuchacz rzuca wyjątek

        await().until(() -> "FAILED".equals(inventoryPublicationOf("ZAM-401").get("STATUS")));
        Map<String, Object> failed = inventoryPublicationOf("ZAM-401");
        assertThat(failed.get("COMPLETION_DATE")).isNull();
        assertThat(failed.get("COMPLETION_ATTEMPTS")).isEqualTo(1);
        assertThat(orders.statusOf("ZAM-401")).contains("NOWE");    // zamówienie zostało: inna transakcja niż słuchacza

        inventory.addProduct("NOW-001", 5);                          // „naprawa”: magazyn poznaje produkt
        incompletePublications.resubmitIncompletePublications(
                publication -> publication.getEvent() instanceof OrderPlaced event && event.orderId().equals("ZAM-401"));

        await().until(() -> "COMPLETED".equals(inventoryPublicationOf("ZAM-401").get("STATUS")));
        assertThat(inventoryPublicationOf("ZAM-401").get("COMPLETION_ATTEMPTS")).isEqualTo(2);
        assertThat(inventory.stockOf("NOW-001")).contains(4);
    }

    private List<Map<String, Object>> publicationsOf(String orderId) {
        return jdbc.sql("SELECT * FROM EVENT_PUBLICATION WHERE SERIALIZED_EVENT LIKE :pattern")
                .param("pattern", "%\"" + orderId + "\"%")
                .query().listOfRows();
    }

    private Map<String, Object> inventoryPublicationOf(String orderId) {
        return publicationsOf(orderId).stream()
                .filter(row -> INVENTORY_LISTENER_ID.equals(row.get("LISTENER_ID")))
                .findFirst().orElseThrow();
    }

    private static List<String> dependencies(ApplicationModules modules, String moduleName) {
        return modules.getModuleByName(moduleName).orElseThrow()
                .getDirectDependencies(modules).uniqueModules()
                .map(module -> module.getIdentifier().toString()).sorted().toList();
    }
}
