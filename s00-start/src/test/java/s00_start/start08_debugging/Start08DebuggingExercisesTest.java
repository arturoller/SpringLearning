package s00_start.start08_debugging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import s00_start.start08_debugging.DebugShop.OrderService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Start08Debugging. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Bez @SpringBootTest: ćwiczenie 2 używa ApplicationContextRunner, a ćwiczenia 1 i 3 to zwykłe testy jednostkowe.
 */
@Tag("cwiczenie")
class Start08DebuggingExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: pierwotna przyczyna to ostatni wyjątek w łańcuchu getCause()")
    void exercise1() {
        Throwable root = new IllegalStateException("magazyn");
        Throwable wrapped = new RuntimeException("warstwa 1", new IllegalArgumentException("warstwa 2", root));
        assertThat(Start08Debugging.exercise1(wrapped)).isSameAs(root);
        assertThat(Start08Debugging.exercise1(root)).as("wyjątek bez przyczyny jest swoją własną przyczyną").isSameAs(root);
    }

    @Test
    @DisplayName("Ćwiczenie 2: poprawiona konfiguracja uruchamia kontekst, a OrderService dostaje powiadamiacz SMS")
    void exercise2() {
        new ApplicationContextRunner().withUserConfiguration(ExerciseConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(OrderService.class).channel()).isEqualTo("SMS");
        });
    }

    @Test
    @DisplayName("Ćwiczenie 3: analizator opisuje błąd z SKU i daje radę; cudze wyjątki ignoruje")
    void exercise3() {
        StockFailureAnalyzer analyzer = new StockFailureAnalyzer();

        FailureAnalysis analysis = analyzer.analyze(
                new IllegalStateException("Start aplikacji nie powiódł się", new OutOfStockException("ELE-002")));
        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("ELE-002");
        assertThat(analysis.getAction()).isNotBlank();
        assertThat(analysis.getCause()).isInstanceOf(OutOfStockException.class);

        assertThat(analyzer.analyze(new IllegalStateException("zupełnie inny błąd"))).isNull();
    }
}
