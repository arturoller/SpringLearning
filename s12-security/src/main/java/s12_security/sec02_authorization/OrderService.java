package s12_security.sec02_authorization;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Serwis zamówień lekcji Sec02Authorization — bezpieczeństwo METOD (sekcja 3). Adnotacje sprawdza proxy (pośrednik CGLIB)
 * tworzone przez {@code @EnableMethodSecurity}: najpierw reguła, dopiero potem nasza metoda (albo odwrotnie przy
 * {@code @PostAuthorize}/{@code @PostFilter}). Wyrażenia w cudzysłowie to SpEL (Spring Expression Language — język wyrażeń
 * Springa): {@code authentication} = zalogowany użytkownik, {@code #username} = parametr metody, {@code returnObject} = wynik,
 * {@code filterObject} = element zwracanej listy.
 */
@Service
public class OrderService {

    private static final List<ShopOrder> ORDERS = List.of(
            new ShopOrder(1, "anna", "Kawa ziarnista", new BigDecimal("39.90")),
            new ShopOrder(2, "anna", "Herbata zielona", new BigDecimal("18.50")),
            new ShopOrder(3, "piotr", "Kawa ziarnista", new BigDecimal("79.80")));

    /** Zamówienia jednego klienta: tylko on sam albo pracownik (EMPLOYEE, a dzięki hierarchii także ADMIN). */
    @PreAuthorize("#username == authentication.name or hasRole('EMPLOYEE')")
    public List<ShopOrder> ordersOf(String username) {
        return ORDERS.stream().filter(order -> order.owner().equals(username)).toList();
    }

    /** Jedno zamówienie po id: reguła sprawdzana PO wykonaniu metody, bo dopiero wynik mówi, czyje to zamówienie. */
    @PostAuthorize("returnObject.owner() == authentication.name or hasRole('EMPLOYEE')")
    public ShopOrder byId(long id) {
        return ORDERS.stream().filter(order -> order.id() == id).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Nie ma zamówienia " + id));
    }

    /** Wszystkie zamówienia, ale Spring usuwa z wyniku cudze (klient widzi tylko swoje, pracownik — wszystkie). */
    @PostFilter("filterObject.owner() == authentication.name or hasRole('EMPLOYEE')")
    public List<ShopOrder> visibleOrders() {
        return ORDERS;    // lista niezmienna (List.of) — w Security 7 @PostFilter i tak zwraca nową, przefiltrowaną listę
    }

    /** Statystyka dla administratora — druga linia obrony obok reguły adresu /sec02/admin/** (sekcja 5). */
    @PreAuthorize("hasRole('ADMIN')")
    public int countAll() {
        return ORDERS.size();
    }
}
