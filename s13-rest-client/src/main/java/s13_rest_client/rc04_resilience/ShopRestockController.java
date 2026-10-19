package s13_rest_client.rc04_resilience;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

/**
 * ShopRestockController = punkty końcowe NASZEGO sklepu do pliku .http (restock = uzupełnienie zapasu):
 * ceny z ponawianiem, zamówienia u dostawcy — źle (bez klucza) i dobrze (z kluczem idempotencji).
 */
@RestController
public class ShopRestockController {

    private final SupplierClient supplier;

    public ShopRestockController(SupplierClient supplier) {
        this.supplier = supplier;
    }

    /** GET /api/shop/prices/ELE-001 → 200 mimo dwóch 503 u dostawcy — @Retryable ponowił w tle. */
    @GetMapping("/api/shop/prices/{sku}")
    public SupplierPrice price(@PathVariable String sku) {
        return supplier.price(sku);
    }

    /** POST /api/shop/restock-unsafe — ZŁY przykład: ponawiany POST bez klucza → duplikat u dostawcy. */
    @PostMapping("/api/shop/restock-unsafe")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierOrderConfirmation restockUnsafe(@RequestBody SupplierOrder order) {
        return supplier.placeOrderUnsafe(order);
    }

    /**
     * POST /api/shop/restock — DOBRY przykład: klucz idempotencji tworzymy RAZ, tu, przed wywołaniem klienta;
     * wszystkie próby ponawiania wysyłają ten sam klucz. W prawdziwym sklepie kluczem byłby np. numer naszego
     * zamówienia zapisany w bazie, żeby przetrwał także restart aplikacji.
     */
    @PostMapping("/api/shop/restock")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierOrderConfirmation restock(@RequestBody SupplierOrder order) {
        return supplier.placeOrder(order, UUID.randomUUID().toString());
    }

    /** GET /api/shop/report — raport magazynowy (najwyżej 2 równoczesne zapytania do dostawcy). */
    @GetMapping("/api/shop/report")
    public StockReport report() {
        return supplier.stockReport();
    }

    @ExceptionHandler({HttpServerErrorException.class, ResourceAccessException.class})
    public ProblemDetail supplierDown(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Dostawca nie odpowiada mimo ponowień — spróbuj później");
    }
}
