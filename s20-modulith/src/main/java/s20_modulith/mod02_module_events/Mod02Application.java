package s20_modulith.mod02_module_events;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import s20_modulith.mod02_module_events.inventory.InventoryManagement;
import s20_modulith.mod02_module_events.orders.OrderManagement;

/**
 * Klasa startowa lekcji Mod02ModuleEvents. Moduły: {@code orders} (publikuje OrderPlaced) i {@code inventory}
 * (słucha OrderPlaced, rezerwuje towar). Profil {@code mod02}: baza H2 jdbc:h2:mem:mod02, migracje Flyway
 * z db/migration/mod02 (także tabela EVENT_PUBLICATION rejestru zdarzeń).
 * Uruchomienie (▶ przy main): zamówienie, krótkie czekanie na asynchronicznego słuchacza, wypisanie stanu.
 */
@SpringBootApplication
public class Mod02Application {

    public static void main(String[] args) throws InterruptedException {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Mod02Application.class)
                .profiles("mod02").web(WebApplicationType.NONE).run(args)) {
            OrderManagement orders = context.getBean(OrderManagement.class);
            InventoryManagement inventory = context.getBean(InventoryManagement.class);
            System.out.println("Stan KSI-001 przed: " + inventory.stockOf("KSI-001").orElseThrow());
            orders.placeOrder("ZAM-001", "KSI-001", 2);
            // Tylko w tym pokazowym main: słuchacz działa w INNYM wątku, więc chwilę czekamy (w testach — Awaitility).
            Thread.sleep(500);
            System.out.println("Stan KSI-001 po:    " + inventory.stockOf("KSI-001").orElseThrow());
        }
    }
}
