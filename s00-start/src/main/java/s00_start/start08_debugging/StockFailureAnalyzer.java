package s00_start.start08_debugging;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * SZKIELET do ĆWICZENIA 3 lekcji Start08Debugging: analizator błędu startu (FailureAnalyzer) dla wyjątku
 * {@link OutOfStockException}. Opis zadania — w Start08Debugging.
 *
 * <p>Klasa NIE jest zarejestrowana w {@code META-INF/spring.factories}, więc żadna aplikacja jej nie użyje —
 * test ćwiczenia wywołuje ją bezpośrednio.</p>
 */
public class StockFailureAnalyzer extends AbstractFailureAnalyzer<OutOfStockException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, OutOfStockException cause) {
        // TODO: zwróć new FailureAnalysis(opis, rada, cause) — opis ma zawierać SKU z wyjątku
        throw new UnsupportedOperationException("TODO");
    }
}
