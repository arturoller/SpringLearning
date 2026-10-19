package s04_web_rest.rest03_responses;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ProductNotFoundException = „nie znaleziono produktu”. {@code @ResponseStatus} (status odpowiedzi) NA KLASIE WYJĄTKU:
 * gdy ten wyjątek wyleci z kontrolera, Spring (ResponseStatusExceptionResolver) odpowie 404 zamiast 500.
 * reason = powód — tekst dołączany do odpowiedzi błędu.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Nie ma takiego produktu")
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String sku) {
        super("Nie ma produktu " + sku);
    }
}
