package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Produkt wycofany ze sprzedaży — {@code @ResponseStatus} z polem {@code reason} (powód), czyli stałym tekstem błędu. */
@ResponseStatus(value = HttpStatus.GONE, reason = "Produkt wycofany ze sprzedaży")
public class DiscontinuedProductException extends RuntimeException {

    public DiscontinuedProductException(String sku) {
        super("Produkt " + sku + " został wycofany");
    }
}
