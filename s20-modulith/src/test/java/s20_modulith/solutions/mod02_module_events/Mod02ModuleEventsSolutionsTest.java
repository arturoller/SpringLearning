package s20_modulith.solutions.mod02_module_events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.test.context.ActiveProfiles;
import s20_modulith.mod02_module_events.Mod02Application;
import s20_modulith.mod02_module_events.inventory.InventoryManagement;
import s20_modulith.mod02_module_events.orders.OrderManagement;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Sprawdzenia z Mod02ModuleEventsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie leży pod klasą startową lekcji, więc wskazujemy ją jawnie: {@code classes = Mod02Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Mod02Application.class,
        properties = "spring.datasource.url=jdbc:h2:mem:mod02-solutions;DB_CLOSE_DELAY=-1")
@ActiveProfiles("mod02")
class Mod02ModuleEventsSolutionsTest {

    @Autowired
    private OrderManagement orders;

    @Autowired
    private InventoryManagement inventory;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EventPublicationRegistry registry;

    @Autowired
    private IncompleteEventPublications incomplete;

    @Autowired
    private CompletedEventPublications completed;

    @Test
    @DisplayName("Wzorzec 1: liczba nieukończonych publikacji")
    void solution1() {
        orders.placeOrder("ZAM-S11", "NOW-S11", 1);
        await().until(() -> "FAILED".equals(inventoryRow("ZAM-S11").get("STATUS")));

        int expected = registry.findIncompletePublications().size();
        assertThat(expected).isPositive();
        assertThat(Mod02ModuleEventsSolutions.solution1(jdbc)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Wzorzec 2: typy zdarzeń, których słucha moduł")
    void solution2() {
        ApplicationModules modules = ApplicationModules.of(Mod02Application.class);
        assertThat(Mod02ModuleEventsSolutions.solution2(modules, "inventory")).containsExactly("OrderPlaced");
        assertThat(Mod02ModuleEventsSolutions.solution2(modules, "orders")).isEmpty();
        assertThatThrownBy(() -> Mod02ModuleEventsSolutions.solution2(modules, "shipping"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Nie ma modułu: shipping");
    }

    @Test
    @DisplayName("Wzorzec 3: ponowienie tylko wybranego zamówienia")
    void solution3() {
        orders.placeOrder("ZAM-S31", "NOW-S31", 1);
        orders.placeOrder("ZAM-S32", "NOW-S32", 1);
        await().until(() -> "FAILED".equals(inventoryRow("ZAM-S31").get("STATUS"))
                && "FAILED".equals(inventoryRow("ZAM-S32").get("STATUS")));

        inventory.addProduct("NOW-S31", 3);
        Mod02ModuleEventsSolutions.solution3(incomplete, "ZAM-S31");

        assertThat(inventoryRow("ZAM-S32").get("STATUS")).isEqualTo("FAILED");
        assertThat(inventoryRow("ZAM-S32").get("COMPLETION_ATTEMPTS")).isEqualTo(1);

        await().until(() -> "COMPLETED".equals(inventoryRow("ZAM-S31").get("STATUS")));
        assertThat(inventory.stockOf("NOW-S31")).contains(2);
    }

    @Test
    @DisplayName("Wzorzec 4: sprzątanie ukończonych publikacji")
    void solution4() {
        orders.placeOrder("ZAM-S41", "KSI-001", 1);
        await().until(() -> "COMPLETED".equals(inventoryRow("ZAM-S41").get("STATUS")));

        int expected = completed.findAll().size();
        assertThat(Mod02ModuleEventsSolutions.solution4(completed)).isEqualTo(expected).isPositive();
        assertThat(completed.findAll()).isEmpty();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NOT NULL")
                .query(Integer.class).single()).isZero();
    }

    private Map<String, Object> inventoryRow(String orderId) {
        return jdbc.sql("SELECT * FROM EVENT_PUBLICATION WHERE SERIALIZED_EVENT LIKE :pattern AND LISTENER_ID LIKE :listener")
                .param("pattern", "%\"" + orderId + "\"%")
                .param("listener", "%InventoryEventHandler%")
                .query().singleRow();
    }
}
