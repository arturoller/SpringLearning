package s20_modulith.mod02_module_events.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import s20_modulith.mod02_module_events.orders.OrderManagement;
import s20_modulith.mod02_module_events.orders.OrderPlaced;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test sekcji 6 lekcji Mod02ModuleEvents: test JEDNEGO modułu ({@code inventory}) w izolacji.
 *
 * <p>{@code @ApplicationModuleTest} (test modułu aplikacji) leży w pakiecie modułu (tu: {@code …inventory}) i startuje
 * kontekst Springa tylko z beanami tego modułu (tryb STANDALONE = samodzielny). Przed startem sprawdza też całą
 * strukturę modułów (jak {@code verify()}). {@code Scenario} (scenariusz) to wstrzykiwany parametr metody: publikuje
 * zdarzenie w transakcji i CZEKA (przez bibliotekę Awaitility, domyślnie do 10 s) na skutek — bez {@code sleep}.
 * Własna baza H2 ({@code mod02-inventory}), żeby dane innych testów lekcji nie wpływały na wynik.</p>
 */
@ApplicationModuleTest
@ActiveProfiles("mod02")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:mod02-inventory;DB_CLOSE_DELAY=-1")
class Mod02InventoryModuleTest {

    private final InventoryManagement inventory;
    private final ApplicationContext context;

    // @ApplicationModuleTest włącza @TestConstructor(autowireMode = ALL): parametry konstruktora testu to beany
    Mod02InventoryModuleTest(InventoryManagement inventory, ApplicationContext context) {
        this.inventory = inventory;
        this.context = context;
    }

    @Test
    @DisplayName("6a. Scenario: publikacja OrderPlaced → czekamy na zdarzenie StockReserved i sprawdzamy jego treść")
    void reservesStockAndPublishesEvent(Scenario scenario) {
        scenario.publish(new OrderPlaced("ZAM-601", "KSI-001", 2))
                .andWaitForEventOfType(StockReserved.class)
                .matching(event -> event.orderId().equals("ZAM-601"))
                .toArriveAndVerify(event -> {
                    assertThat(event.sku()).isEqualTo("KSI-001");
                    assertThat(event.quantity()).isEqualTo(2);
                });
    }

    @Test
    @DisplayName("6b. Scenario: publikacja OrderPlaced → czekamy na zmianę stanu magazynu")
    void decreasesStock(Scenario scenario) {
        int before = inventory.stockOf("ELE-003").orElseThrow();

        scenario.publish(new OrderPlaced("ZAM-602", "ELE-003", 5))
                .andWaitForStateChange(() -> inventory.stockOf("ELE-003").orElseThrow(), stock -> stock == before - 5)
                .andVerify(stock -> assertThat(stock).isEqualTo(before - 5));
    }

    @Test
    @DisplayName("6c. Tryb STANDALONE: w kontekście są beany modułu inventory, a beanów modułu orders nie ma")
    void bootstrapsOnlyThisModule() {
        assertThat(context.getBeanNamesForType(InventoryManagement.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(OrderManagement.class)).isEmpty();
    }
}
