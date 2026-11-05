package s11_testing.test05_test_configuration;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Licznik wyświetleń produktów — singleton (jeden obiekt na cały kontekst) ZE STANEM. Właśnie taki bean psuje
 * testy, które dzielą kontekst z pamięci podręcznej: to, co jeden test nabije, widzi następny (sekcja 4 lekcji).
 */
@Component
public class ViewCounter {

    private final AtomicInteger views = new AtomicInteger(); // atomic = niepodzielny, bezpieczny dla wątków

    public int register() {
        return views.incrementAndGet();
    }

    public int count() {
        return views.get();
    }

    /** Zerowanie stanu — tańsza alternatywa dla {@code @DirtiesContext} (wywoływana w {@code @BeforeEach}). */
    public void reset() {
        views.set(0);
    }
}
