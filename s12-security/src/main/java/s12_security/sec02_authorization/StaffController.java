package s12_security.sec02_authorization;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * StaffController = zaplecze sklepu: kadry (rola HR lub ADMIN), raporty (uprawnienie report:read) i statystyki,
 * o których ZAPOMNIELIŚMY w regułach — dlatego obejmuje je {@code anyRequest().denyAll()} (sekcja 5 lekcji).
 */
@RestController
@RequestMapping("/api")
public class StaffController {

    /** Kadry: imię, dział, pensja (zł). */
    @GetMapping("/hr/employees")
    public List<Map<String, Object>> employees() {
        return List.of(
                Map.of("name", "Anna Nowak", "dept", "IT", "salary", 14500),
                Map.of("name", "Piotr Kowalski", "dept", "IT", "salary", 9800),
                Map.of("name", "Katarzyna Wiśniewska", "dept", "HR", "salary", 7200),
                Map.of("name", "Tomasz Wójcik", "dept", "SPRZEDAZ", "salary", 8900));
    }

    /** Raport sprzedaży — wymaga uprawnienia report:read (nie roli!). */
    @GetMapping("/reports/sales")
    public Map<String, Object> salesReport() {
        return Map.of("report", "Sprzedaż tygodniowa", "orders", 84);
    }

    /** Statystyki — nowy punkt końcowy, którego nikt nie dopisał do reguł. */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("visitorsToday", 1250);
    }
}
