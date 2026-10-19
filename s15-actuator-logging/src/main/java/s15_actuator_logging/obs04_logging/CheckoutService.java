package s15_actuator_logging.obs04_logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Składanie zamówienia — pokazuje DOBRE sposoby logowania: parametry {@code {}} zamiast sklejania tekstu,
 * poziomy dopasowane do wagi zdarzenia, wyjątek jako OSTATNI argument oraz maskowanie danych osobowych.
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final PaymentService payments;
    private final AtomicInteger sequence = new AtomicInteger();

    public CheckoutService(PaymentService payments) {
        this.payments = payments;
    }

    /** Składa zamówienie i zwraca jego numer albo "BLAD", gdy płatność się nie udała. */
    public String checkout(NewCheckout checkout) {
        String orderId = "ZAM-%03d".formatted(sequence.incrementAndGet());
        // DEBUG = szczegóły dla programisty; domyślnie wyłączony (domyślny poziom aplikacji to INFO)
        log.debug("Sprawdzam dostępność {} (sztuk: {})", checkout.sku(), checkout.quantity());
        try {
            payments.charge(orderId, checkout.quantity() * checkout.priceGrosze());
        } catch (IllegalStateException e) {
            // ERROR + wyjątek jako OSTATNI argument: logger wypisze komunikat ORAZ ślad stosu (stack trace)
            log.error("Płatność zamówienia {} nie powiodła się", orderId, e);
            return "BLAD";
        }
        // INFO = zdarzenie biznesowe; e-mail klienta MASKUJEMY (dane osobowe)
        log.info("Złożono zamówienie {} ({} x {}), klient {}", orderId, checkout.quantity(), checkout.sku(),
                maskEmail(checkout.email()));
        return orderId;
    }

    /** Maskuje e-mail do logów: "jan@example.com" → "j***@example.com". Brak e-maila → "brak". */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "brak";
        }
        int at = email.indexOf('@');
        if (at < 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
