package s20_modulith.mod02_module_events;

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
import s20_modulith.mod02_module_events.inventory.InventoryManagement;
import s20_modulith.mod02_module_events.orders.OrderManagement;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Testy ĆWICZEŃ lekcji Mod02ModuleEvents. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Cała aplikacja z własną bazą H2 ({@code mod02-exercises}); słuchacze są asynchroniczni, więc czekamy (Awaitility).
 */
@Tag("cwiczenie")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:mod02-exercises;DB_CLOSE_DELAY=-1")
@ActiveProfiles("mod02")
class Mod02ModuleEventsExercisesTest {

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
    @DisplayName("Ćwiczenie 1: liczba nieukończonych publikacji")
    void exercise1() {
        orders.placeOrder("ZAM-E11", "NOW-E11", 1);                            // nieznany produkt → FAILED
        await().until(() -> "FAILED".equals(inventoryRow("ZAM-E11").get("STATUS")));

        int expected = registry.findIncompletePublications().size();           // to samo przez API rejestru
        assertThat(expected).isPositive();
        assertThat(Mod02ModuleEvents.exercise1(jdbc)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Ćwiczenie 2: typy zdarzeń, których słucha moduł")
    void exercise2() {
        ApplicationModules modules = ApplicationModules.of(Mod02Application.class);
        assertThat(Mod02ModuleEvents.exercise2(modules, "inventory")).containsExactly("OrderPlaced");
        assertThat(Mod02ModuleEvents.exercise2(modules, "orders")).isEmpty();
        assertThatThrownBy(() -> Mod02ModuleEvents.exercise2(modules, "shipping"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Nie ma modułu: shipping");
    }

    @Test
    @DisplayName("Ćwiczenie 3: ponowienie tylko wybranego zamówienia")
    void exercise3() {
        orders.placeOrder("ZAM-E31", "NOW-E31", 1);
        orders.placeOrder("ZAM-E32", "NOW-E32", 1);
        await().until(() -> "FAILED".equals(inventoryRow("ZAM-E31").get("STATUS"))
                && "FAILED".equals(inventoryRow("ZAM-E32").get("STATUS")));

        inventory.addProduct("NOW-E31", 3);                                      // naprawiamy tylko pierwszy
        Mod02ModuleEvents.exercise3(incomplete, "ZAM-E31");

        // ZAM-E32 nie był ponawiany: wciąż FAILED po jednym podejściu (ponowienie od razu zmienia STATUS i licznik)
        assertThat(inventoryRow("ZAM-E32").get("STATUS")).isEqualTo("FAILED");
        assertThat(inventoryRow("ZAM-E32").get("COMPLETION_ATTEMPTS")).isEqualTo(1);

        await().until(() -> "COMPLETED".equals(inventoryRow("ZAM-E31").get("STATUS")));
        assertThat(inventory.stockOf("NOW-E31")).contains(2);
    }

    @Test
    @DisplayName("Ćwiczenie 4: sprzątanie ukończonych publikacji")
    void exercise4() {
        orders.placeOrder("ZAM-E41", "KSI-001", 1);
        await().until(() -> "COMPLETED".equals(inventoryRow("ZAM-E41").get("STATUS")));

        int expected = completed.findAll().size();
        assertThat(Mod02ModuleEvents.exercise4(completed)).isEqualTo(expected).isPositive();
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
