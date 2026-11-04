package s06_error_handling.err05_custom_exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Wyjątek ze statusem wpisanym ADNOTACJĄ ({@code @ResponseStatus}, znaną z Rest03Responses). Wygodne, ale wyjątek zaczyna
 * "znać HTTP" — w tej lekcji to PRZECIWPRZYKŁAD (sekcja 2): hierarchii ShopException nie oznaczamy żadną adnotacją.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(long supplierId) {
        super("Nie ma dostawcy " + supplierId);
    }
}
