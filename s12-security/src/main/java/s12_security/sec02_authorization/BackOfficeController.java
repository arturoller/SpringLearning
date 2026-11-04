package s12_security.sec02_authorization;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zaplecze sklepu lekcji Sec02Authorization: raporty (uprawnienie REPORTS_READ), panel administratora (rola ADMIN) i adres
 * "zapomniany" w regułach ({@code /sec02/internal/debug}) — ten zamyka {@code anyRequest().denyAll()} (sekcja 5).
 */
@RestController
public class BackOfficeController {

    private final OrderService orders;

    public BackOfficeController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/sec02/reports/sales")
    public String salesReport() {
        return "Sprzedaż dzisiaj: 138,20 zł";
    }

    @GetMapping("/sec02/admin/stats")
    public String adminStats() {
        return "Liczba zamówień: " + orders.countAll();
    }

    @GetMapping("/sec02/internal/debug")
    public String debug() {
        return "Wewnętrzne dane diagnostyczne";
    }
}
