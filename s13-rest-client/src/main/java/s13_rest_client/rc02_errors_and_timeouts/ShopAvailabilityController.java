package s13_rest_client.rc02_errors_and_timeouts;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * ShopAvailabilityController = punkt końcowy NASZEGO sklepu: „czy dostawca ma ten produkt?”.
 * Woła dostawcę przez SupplierStockClient i TŁUMACZY jego kłopoty na odpowiedzi naszego API:
 * <ul>
 *   <li>dostawca nie zna produktu → 404 Not Found (ProblemDetail),</li>
 *   <li>dostawca padł, jest wolny albo nieosiągalny → 503 Service Unavailable (ProblemDetail) — a NIE 500.</li>
 * </ul>
 * {@code @ExceptionHandler} w kontrolerze — s06_error_handling/Err02ExceptionHandler.
 */
@RestController
public class ShopAvailabilityController {

    private final SupplierStockClient supplier;

    public ShopAvailabilityController(SupplierStockClient supplier) {
        this.supplier = supplier;
    }

    /** GET /api/shop/availability/ELE-001 → 200 OK i {"sku":"ELE-001","available":7}. */
    @GetMapping("/api/shop/availability/{sku}")
    public StockLevel availability(@PathVariable String sku) {
        return supplier.stock(sku);
    }

    @ExceptionHandler(ProductNotFoundAtSupplierException.class)
    public ProblemDetail productNotFound(ProductNotFoundAtSupplierException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Dostawca nie zna produktu " + ex.getSku());
    }

    @ExceptionHandler(SupplierUnavailableException.class)
    public ProblemDetail supplierUnavailable(SupplierUnavailableException ex) {
        // Szczegółów technicznych (adres dostawcy, treść jego błędu) NIE pokazujemy klientowi — tylko w logach.
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Dostawca chwilowo niedostępny — spróbuj za chwilę");
    }
}
