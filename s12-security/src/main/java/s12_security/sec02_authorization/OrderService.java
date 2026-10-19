package s12_security.sec02_authorization;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * OrderService = zamówienia klientów-pracowników, zabezpieczone NA POZIOMIE METOD (sekcja 4 lekcji).
 *
 * <p>Reguła w łańcuchu filtrów mówi tylko „/api/orders/** — zalogowani”. Ale piotr nie powinien czytać zamówień
 * anny — a tego po samym adresie nie da się porządnie sprawdzić. Dlatego decyzja zapada tutaj, w metodzie, gdzie
 * znamy parametr i wynik. {@code @PreAuthorize} (autoryzuj przed) i {@code @PostAuthorize} (autoryzuj po) działają
 * dzięki {@code @EnableMethodSecurity} w Sec02SecurityConfig: Spring opakowuje ten bean w proxy (pośrednika), które
 * sprawdza wyrażenie przed wywołaniem (albo po nim) metody.</p>
 */
@Service
public class OrderService {

    /** Zamówienie: numer, właściciel (login), kwota w złotych. */
    public record Order(String id, String owner, BigDecimal total) {
    }

    private final Map<String, List<Order>> ordersByOwner = Map.of(
            "anna", List.of(new Order("ZAM-001", "anna", new BigDecimal("5499.99"))),
            "piotr", List.of(new Order("ZAM-002", "piotr", new BigDecimal("64.99")),
                    new Order("ZAM-003", "piotr", new BigDecimal("79.00"))));

    /**
     * Zamówienia właściciela. SpEL (Spring Expression Language — język wyrażeń Springa):
     * {@code #owner} = parametr metody o nazwie owner, {@code authentication.name} = login zalogowanego.
     * Czyli: „swoje zamówienia widzi każdy, cudze — tylko ADMIN”.
     */
    @PreAuthorize("#owner == authentication.name or hasRole('ADMIN')")
    public List<Order> ordersOf(String owner) {
        return ordersByOwner.getOrDefault(owner, List.of());
    }

    /**
     * Jedno zamówienie po numerze. Właściciela znamy dopiero PO odczytaniu — dlatego {@code @PostAuthorize}:
     * metoda się wykona, a wynik ({@code returnObject} — zwrócony obiekt) zostanie sprawdzony przed oddaniem.
     */
    @PostAuthorize("returnObject.owner() == authentication.name or hasRole('ADMIN')")
    public Order findById(String id) {
        return ordersByOwner.values().stream()
                .flatMap(List::stream)
                .filter(order -> order.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Brak zamówienia " + id));
    }

    /**
     * Podsumowanie — BEZ adnotacji, woła {@code ordersOf} z WNĘTRZA tej samej klasy.
     * PUŁAPKA: wywołanie {@code this.ordersOf(...)} omija proxy, więc {@code @PreAuthorize} NIE jest sprawdzane
     * i piotr zobaczy podsumowanie zamówień anny (Sec02AuthorizationTest, sekcja 4).
     */
    public String summaryOf(String owner) {
        List<Order> orders = ordersOf(owner);   // self-invocation = wywołanie samego siebie — bez proxy!
        BigDecimal sum = orders.stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return owner + ": " + orders.size() + " zamówień na " + sum + " zł";
    }
}
