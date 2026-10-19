package s00_start.solutions.start08_debugging;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import s00_start.start08_debugging.OutOfStockException;

/**
 * Rozwiązanie wzorcowe ćwiczenia 3. Aby Boot używał analizatora w prawdziwej aplikacji, rejestrujemy go w pliku
 * META-INF/spring.factories: {@code org.springframework.boot.diagnostics.FailureAnalyzer=pelna.nazwa.Klasy}.
 */
public class SolutionStockFailureAnalyzer extends AbstractFailureAnalyzer<OutOfStockException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, OutOfStockException cause) {
        return new FailureAnalysis(
                "Aplikacja nie może wystartować, bo produkt " + cause.getSku() + " nie ma towaru w magazynie.",
                "Uzupełnij stan magazynu produktu " + cause.getSku() + " albo wyłącz jego sprzedaż.",
                cause);
    }
}
