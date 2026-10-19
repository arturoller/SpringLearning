package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kontroler REST pokazujący open-in-view (OSIV — „otwarty EntityManager aż do widoku”). Celowo ZŁY wzorzec
 * w {@code order}: serwis oddaje encję, a kontroler — już PO transakcji — sięga po leniwe pole klienta.
 * Działa tylko dlatego, że open-in-view jest włączone. Poprawny wzorzec (DTO budowane w serwisie): Rel06EntityToDto.
 */
@RestController
@RequestMapping("/api/rel04/orders")
public class OrderController {

    private final OrderService orderService;
    private final ConnectionPoolProbe probe;

    public OrderController(OrderService orderService, ConnectionPoolProbe probe) {
        this.orderService = orderService;
        this.probe = probe;
    }

    /** Widok zamówienia; busyConnections — ile połączeń z puli jest zajętych, gdy kontroler kończy pracę. */
    public record OrderView(String number, String customerName, int busyConnections) {
    }

    /** Sam status — BEZ dotykania leniwych pól. */
    public record StatusView(String number, String status, int busyConnections) {
    }

    @GetMapping("/{number}")
    public OrderView order(@PathVariable String number) {
        ShopOrder order = orderService.findOrder(number);        // transakcja serwisu JUŻ się skończyła
        String customerName = order.getCustomer().getName();     // leniwe pole PO transakcji!
        return new OrderView(order.getNumber(), customerName, probe.busyConnections());
    }

    @GetMapping("/{number}/status")
    public StatusView status(@PathVariable String number) {
        ShopOrder order = orderService.findOrder(number);
        return new StatusView(order.getNumber(), order.getStatus(), probe.busyConnections());
    }
}
