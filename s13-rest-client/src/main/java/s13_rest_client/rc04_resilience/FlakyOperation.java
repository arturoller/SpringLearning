package s13_rest_client.rc04_resilience;

import org.springframework.resilience.annotation.Retryable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * FlakyOperation = mała, „kapryśna” operacja BEZ HTTP — do pokazania, KIEDY @Retryable działa, a kiedy jest po cichu
 * ignorowany (Rc04ResilienceCoreTest). Pierwsze {@code failuresBeforeSuccess} wywołań rzuca wyjątek, potem się udaje.
 * Nie jest {@code @Component} — testy rejestrują ją same w małym kontekście Springa (z {@code @EnableResilientMethods}
 * albo bez) albo tworzą przez {@code new}.
 */
public class FlakyOperation {

    private final int failuresBeforeSuccess;
    final AtomicInteger attempts = new AtomicInteger();   // celowo bez private — test pokazuje pułapkę pól proxy

    public FlakyOperation(int failuresBeforeSuccess) {
        this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    @Retryable(maxRetries = 3, delay = 0)   // delay = 0: bez czekania między próbami (tylko na potrzeby przykładu)
    public String call() {
        int attempt = attempts.incrementAndGet();
        if (attempt <= failuresBeforeSuccess) {
            throw new IllegalStateException("awaria przejściowa nr " + attempt);
        }
        return "OK w próbie " + attempt;
    }

    /** Liczba prób — czytaj PRZEZ METODĘ, nie przez pole (pola obiektu proxy są puste — patrz lekcja, sekcja 3). */
    public int attempts() {
        return attempts.get();
    }
}
