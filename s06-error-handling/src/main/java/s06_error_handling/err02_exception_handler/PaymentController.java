package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kontroler płatności z handlerami WEWNĄTRZ klasy: {@code @ExceptionHandler} (obsługa wyjątku) obowiązuje tylko dla
 * metod TEGO kontrolera. Dwa handlery z hierarchii: PaymentException i jej podklasa CardDeclinedException.
 */
@RestController
public class PaymentController {

    /** POST /api/payments/card → CardDeclinedException; /other → PaymentException; /bug → IllegalArgumentException; /refund → UnsupportedOperationException. */
    @PostMapping("/api/payments/{kind}")
    public String pay(@PathVariable String kind) {
        return switch (kind) {
            case "card" -> throw new CardDeclinedException("4242");
            case "other" -> throw new PaymentException("Płatność nie powiodła się");
            case "bug" -> throw new IllegalArgumentException("Nieznany sposób płatności");
            case "refund" -> throw new UnsupportedOperationException("Zwroty nie są jeszcze obsługiwane");
            default -> "OPLACONE";
        };
    }

    /** Handler ogólny: każdy PaymentException, który nie ma bardziej szczegółowego handlera. Odpowiedź budujemy ręcznie. */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorBody> onPayment(PaymentException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(new ErrorBody("PLATNOSC", e.getMessage()));
    }

    /** Handler szczegółowy: wygrywa dla CardDeclinedException, bo jej typ jest „bliższy” rzuconemu wyjątkowi. Status ustawia adnotacja. */
    @ExceptionHandler(CardDeclinedException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ErrorBody onCardDeclined(CardDeclinedException e) {
        return new ErrorBody("KARTA_ODRZUCONA", e.getMessage());
    }

    /** Handler dla wyjątku Javy: IllegalArgumentException → 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorBody onBadArgument(IllegalArgumentException e) {
        return new ErrorBody("ZLY_ARGUMENT", e.getMessage());
    }

    /**
     * PUŁAPKA: handler BEZ {@code @ResponseStatus} i bez ResponseEntity odpowiada statusem 200 OK — mimo że w ciele jest błąd!
     * Przykład z błędem w sztuce: patrz test Err02ExceptionHandlerTest (punkt 9).
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ErrorBody onUnsupported(UnsupportedOperationException e) {
        return new ErrorBody("NIEOBSLUGIWANE", e.getMessage());
    }
}
