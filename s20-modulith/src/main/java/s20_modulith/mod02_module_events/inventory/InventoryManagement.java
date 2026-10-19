package s20_modulith.mod02_module_events.inventory;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * API modułu {@code inventory} (inventory = stan magazynowy): odczyt stanu i przyjęcie nowego towaru.
 * Rezerwacja towaru po złożeniu zamówienia NIE jest częścią API — robi ją słuchacz zdarzeń z pakietu
 * {@code internal}, sam z siebie.
 */
@Service
public class InventoryManagement {

    private final JdbcClient jdbc;

    public InventoryManagement(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Liczba sztuk na stanie albo pusty Optional, gdy magazyn nie zna produktu. */
    public Optional<Integer> stockOf(String sku) {
        return jdbc.sql("SELECT quantity FROM stock WHERE sku = :sku")
                .param("sku", sku)
                .query(Integer.class)
                .optional();
    }

    /** Przyjmuje do magazynu nowy produkt z podaną liczbą sztuk (np. towar, którego magazyn jeszcze nie znał). */
    public void addProduct(String sku, int quantity) {
        jdbc.sql("INSERT INTO stock (sku, quantity) VALUES (:sku, :quantity)")
                .param("sku", sku)
                .param("quantity", quantity)
                .update();
    }
}
