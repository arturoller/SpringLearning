package s13_rest_client.rc01_rest_client;

import org.springframework.http.CacheControl;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FakeSupplierController = UDAWANY serwer dostawcy (fake = udawany). W prawdziwym życiu to cudza aplikacja w internecie
 * (np. https://api.dostawca.example). Tu działa w tej samej aplikacji, żeby lekcja nie potrzebowała sieci:
 * plik s13-rest-client/http/rc01_rest_client.http pyta go wprost, a SupplierClient — przez RestClient.
 *
 * <p>Zasady „dostawcy”: każde zapytanie musi mieć nagłówek {@code X-Shop-Id} (identyfikator sklepu u dostawcy) —
 * bez niego dostawca odpowiada 400 Bad Request. Dlatego klient ustawia go jako nagłówek DOMYŚLNY.</p>
 */
@RestController
@RequestMapping("/supplier/api")
public class FakeSupplierController {

    /** Nazwa nagłówka z identyfikatorem sklepu — wymaga go każdy punkt końcowy (endpoint) dostawcy. */
    public static final String SHOP_ID_HEADER = "X-Shop-Id";

    private final Map<String, SupplierOffer> offers = new TreeMap<>();      // TreeMap = posortowane po sku
    private final Map<String, String> categories = new TreeMap<>();         // sku → kategoria
    private final AtomicLong nextOrderNumber = new AtomicLong(1001);        // numery zamówień DOST-1001, DOST-1002...

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

    /**
     * GET /supplier/api/offers/ELE-001 → 200 OK + oferta + nagłówek Cache-Control: max-age=60
     * („tę odpowiedź wolno trzymać w pamięci 60 sekund”); nieznany sku → 404 Not Found.
     */
    @GetMapping("/offers/{sku}")
    public ResponseEntity<SupplierOffer> offer(@RequestHeader(SHOP_ID_HEADER) String shopId, @PathVariable String sku) {
        SupplierOffer offer = offers.get(sku);
        if (offer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60))).body(offer);
    }

    /** GET /supplier/api/offers?category=ELEKTRONIKA → lista ofert z kategorii (bez parametru — wszystkie). */
    @GetMapping("/offers")
    public List<SupplierOffer> offers(@RequestHeader(SHOP_ID_HEADER) String shopId,
                                      @RequestParam(required = false) String category) {
        return offers.values().stream()
                .filter(offer -> category == null || category.equals(categories.get(offer.sku())))
                .toList();
    }

    /**
     * POST /supplier/api/orders z JSON-em {"sku":"KSI-001","quantity":3} → 201 Created, nagłówek Location
     * i potwierdzenie. Nieznany sku albo ilość mniejsza niż 1 → 400 Bad Request.
     */
    @PostMapping("/orders")
    public ResponseEntity<SupplierOrderConfirmation> order(@RequestHeader(SHOP_ID_HEADER) String shopId,
                                                           @RequestBody SupplierOrder order) {
        if (!offers.containsKey(order.sku()) || order.quantity() < 1) {
            return ResponseEntity.badRequest().build();
        }
        String orderId = "DOST-" + nextOrderNumber.getAndIncrement();
        SupplierOrderConfirmation confirmation =
                new SupplierOrderConfirmation(orderId, order.sku(), order.quantity(), "PRZYJETE");
        return ResponseEntity.created(URI.create("/supplier/api/orders/" + orderId)).body(confirmation);
    }
}
