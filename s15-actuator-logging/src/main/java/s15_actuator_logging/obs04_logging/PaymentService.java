package s15_actuator_logging.obs04_logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Udawana płatność. Własny logger (rejestrator logów) tej klasy ma nazwę z pełną nazwą klasy — dzięki temu
 * poziomem logowania można sterować osobno dla każdej klasy i pakietu ({@code logging.level.<nazwa>}).
 */
@Service
public class PaymentService {

    // Logger: SLF4J (Simple Logging Facade for Java = prosta fasada logowania) to interfejs, pod spodem pracuje
    // Logback (domyślny w Boot). static final, bo logger jest jeden na klasę, a nie na obiekt.
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** Obciąża płatność kwotą w groszach; kwota niedodatnia to błąd. */
    public void charge(String orderId, long amountGrosze) {
        log.debug("Obciążam płatność zamówienia {} kwotą {} gr", orderId, amountGrosze);
        if (amountGrosze <= 0) {
            throw new IllegalStateException("Kwota płatności musi być dodatnia, a jest " + amountGrosze + " gr");
        }
    }
}
