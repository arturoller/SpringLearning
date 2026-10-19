package s13_rest_client.rc03_http_interfaces;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FakeSupplierController = UDAWANY serwer dostawcy dla lekcji Rc03 (to samo API co w Rc01: oferty i zamówienia).
 * Każde zapytanie musi mieć nagłówek X-Shop-Id — inaczej 400 Bad Request. Klient-interfejs SupplierApi dostaje go
 * z właściwości grupy: spring.http.serviceclient.dostawca.default-header.X-Shop-Id.
 */
@RestController
@RequestMapping("/supplier/api")
public class FakeSupplierController {

    private final Map<String, SupplierOffer> offers = new TreeMap<>();
    private final Map<String, String> categories = new TreeMap<>();
    private final AtomicLong nextOrderNumber = new AtomicLong(1001);

    public FakeSupplierController() {
        add("ELE-001", "Laptop Pro 14", "5499.99", 7, "ELEKTRONIKA");
        add("ELE-002", "Smartfon X", "2999.00", 0, "ELEKTRONIKA");
        add("ELE-003", "Słuchawki BT", "349.90", 25, "ELEKTRONIKA");
        add("SPO-001", "Kawa ziarnista 1kg", "64.99", 120, "SPOZYWCZE");
        add("KSI-001", "Czysty kod", "79.00", 15, "KSIAZKI");
    }

    private void add(String sku, String name, String price, int available, String category) {
        offers.put(sku, new SupplierOffer(sku, name, new BigDecimal(price), available));
        categories.put(sku, category);
    }

    @GetMapping("/offers/{sku}")
    public ResponseEntity<SupplierOffer> offer(@RequestHeader("X-Shop-Id") String shopId, @PathVariable String sku) {
        SupplierOffer offer = offers.get(sku);
        return offer == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(offer);
    }

    @GetMapping("/offers")
    public List<SupplierOffer> offers(@RequestHeader("X-Shop-Id") String shopId,
                                      @RequestParam(required = false) String category) {
        return offers.values().stream()
                .filter(offer -> category == null || category.equals(categories.get(offer.sku())))
                .toList();
    }

    @PostMapping("/orders")
    public ResponseEntity<SupplierOrderConfirmation> order(@RequestHeader("X-Shop-Id") String shopId,
                                                           @RequestBody SupplierOrder order) {
        if (!offers.containsKey(order.sku()) || order.quantity() < 1) {
            return ResponseEntity.badRequest().build();
        }
        String orderId = "DOST-" + nextOrderNumber.getAndIncrement();
        return ResponseEntity.created(URI.create("/supplier/api/orders/" + orderId))
                .body(new SupplierOrderConfirmation(orderId, order.sku(), order.quantity(), "PRZYJETE"));
    }
}
