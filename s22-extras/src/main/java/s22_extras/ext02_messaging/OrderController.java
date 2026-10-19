package s22_extras.ext02_messaging;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Punkty końcowe lekcji Ext02Messaging do sprawdzania ręcznego (s22-extras/http/ext02_messaging.http).
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderEventPublisher publisher;
    private final WarehouseListener warehouse;
    private final InvoiceListener invoices;

    public OrderController(OrderEventPublisher publisher, WarehouseListener warehouse, InvoiceListener invoices) {
        this.publisher = publisher;
        this.warehouse = warehouse;
        this.invoices = invoices;
    }

    /**
     * Przyjmuje zamówienie i wysyła zdarzenie. 202 Accepted = „przyjęte do realizacji” — reszta (magazyn, faktura)
     * dzieje się asynchronicznie, po odpowiedzi.
     */
    @PostMapping
    public ResponseEntity<Void> place(@RequestBody OrderPlaced order) {
        publisher.publish(order);
        return ResponseEntity.accepted().build();
    }

    /** Co odebrały obie grupy konsumentów (od startu aplikacji). */
    @GetMapping("/received")
    public List<ReceivedOrder> received() {
        List<ReceivedOrder> all = new ArrayList<>(warehouse.received());
        all.addAll(invoices.received());
        return all;
    }
}
