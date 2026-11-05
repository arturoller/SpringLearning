package s06_error_handling.err04_problem_detail;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;

/**
 * Wyjątek, który SAM wie, jak wygląda jego odpowiedź: dziedziczy po {@code ErrorResponseException} (error response =
 * odpowiedź błędu), więc implementuje interfejs {@code ErrorResponse} i niesie w sobie gotowy {@code ProblemDetail}.
 * Dzięki temu Spring zamienia go na {@code application/problem+json} bez ani jednego handlera napisanego przez nas
 * (robi to {@code ResponseEntityExceptionHandler}, a gdy go nie ma — wbudowany mechanizm z sekcji 3).
 * Status 410 Gone (gone = "nie ma i nie będzie", zasób usunięty na stałe).
 */
public class ProductArchivedException extends ErrorResponseException {

    public ProductArchivedException(long productId) {
        // Konstruktor z 5 parametrami: status, gotowe ciało, przyczyna (brak), kod komunikatu (null = domyślny) i argumenty {0}…
        // dla komunikatów z pliku messages (sekcja 5). Treść w kodzie jest wartością zapasową, gdy pliku z komunikatami nie ma.
        super(HttpStatus.GONE, ProblemDetail.forStatusAndDetail(HttpStatus.GONE, "Produkt " + productId + " został wycofany z oferty"),
                null, null, new Object[]{productId});
        setType(URI.create("https://sklep.example/errors/product-archived"));
        getBody().setProperty("code", "PRODUCT_ARCHIVED");
    }
}
