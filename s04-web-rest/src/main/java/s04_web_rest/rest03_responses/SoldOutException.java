package s04_web_rest.rest03_responses;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Wyjątek z przypisanym kodem HTTP (sekcja 2 lekcji). Gdy kontroler go rzuci, Spring odpowie 409 Conflict —
 * bez żadnego kodu obsługi w kontrolerze. (Pełna obsługa błędów: s06_error_handling/Err05CustomExceptions.)
 */
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Towar wyprzedany")
public class SoldOutException extends RuntimeException {

    public SoldOutException(long productId) {
        super("Produkt " + productId + " jest wyprzedany");
    }
}
