package s15_actuator_logging.obs02_health_indicators;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Udawany magazyn sklepu — w prawdziwej aplikacji to byłby klient systemu magazynowego (baza, API dostawcy).
 * Przełącznik {@code available} pozwala w teście i w pliku .http „zepsuć” magazyn bez żadnej sieci.
 */
@Component   // component = składnik (bean tworzony przez skanowanie)
public class Warehouse {

    private final AtomicBoolean available = new AtomicBoolean(true);   // atomic = niepodzielny (bezpieczny dla wątków)

    public boolean isAvailable() {
        return available.get();
    }

    public void setAvailable(boolean value) {
        available.set(value);
    }

    /** Liczba pozycji w magazynie (stała — to tylko przykład). */
    public int itemCount() {
        return 14;   // tyle produktów ma sklep (AGENT_KIT 5a)
    }
}
