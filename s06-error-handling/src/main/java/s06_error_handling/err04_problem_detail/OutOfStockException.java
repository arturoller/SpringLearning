package s06_error_handling.err04_problem_detail;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

/**
 * Brak towaru w magazynie. Dziedziczy po {@code ErrorResponseException} (wyjątek, który SAM niesie gotowy ProblemDetail
 * i kod statusu) — dzięki temu ResponseEntityExceptionHandler (także wbudowany w Boota) umie go zamienić na odpowiedź
 * bez żadnego dodatkowego handlera.
 */
public class OutOfStockException extends ErrorResponseException {

    public OutOfStockException(String sku, int available, int requested) {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Brak towaru " + sku + ": dostępne " + available + ", żądane " + requested),
                null);
        setTitle("Brak towaru w magazynie");
        setType(URI.create("https://sklep.example.test/problemy/brak-towaru"));
        getBody().setProperty("sku", sku);   // własne pole rozszerzające (extension member) — trafia do JSON-a na wierzch
        getBody().setProperty("available", available);
    }
}
