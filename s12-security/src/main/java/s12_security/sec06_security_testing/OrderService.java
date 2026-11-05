package s12_security.sec06_security_testing;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serwis zamówień lekcji Sec06SecurityTesting z regułami na metodach (Sec02Authorization, sekcja 3). Sekcja 3 lekcji testuje
 * go BEZ HTTP — samą warstwę serwisu.
 */
@Service
public class OrderService {

    /** Zamówienie: numer, właściciel, produkt. */
    public record ShopOrder(long id, String owner, String product) {
    }

    private static final List<ShopOrder> ORDERS = List.of(
            new ShopOrder(1, "anna", "Kawa ziarnista"),
            new ShopOrder(2, "anna", "Herbata zielona"),
            new ShopOrder(3, "piotr", "Kawa ziarnista"));

    /** Zamówienia klienta: on sam albo pracownik (dzięki hierarchii także ADMIN). */
    @PreAuthorize("#username == authentication.name or hasRole('EMPLOYEE')")
    public List<ShopOrder> ordersOf(String username) {
        return ORDERS.stream().filter(order -> order.owner().equals(username)).toList();
    }

    /** Anulowanie dowolnego zamówienia — tylko ADMIN. */
    @PreAuthorize("hasRole('ADMIN')")
    public String cancel(long id) {
        return "Anulowano zamówienie " + id;
    }
}
