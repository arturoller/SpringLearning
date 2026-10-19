package s00_start.solutions.start08_debugging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import s00_start.start08_debugging.DebugShop.OrderService;
import s00_start.start08_debugging.OutOfStockException;

import static org.assertj.core.api.Assertions.assertThat;

/** Te same sprawdzenia co w Start08DebuggingExercisesTest, ale na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Start08DebuggingSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: pierwotna przyczyna to ostatni wyjątek w łańcuchu getCause()")
    void solution1() {
        Throwable root = new IllegalStateException("magazyn");
        Throwable wrapped = new RuntimeException("warstwa 1", new IllegalArgumentException("warstwa 2", root));
        assertThat(Start08DebuggingSolutions.solution1(wrapped)).isSameAs(root);
        assertThat(Start08DebuggingSolutions.solution1(root)).isSameAs(root);
    }

    @Test
    @DisplayName("Wzorzec 2: z @Primary kontekst startuje, a OrderService dostaje powiadamiacz SMS")
    void solution2() {
        new ApplicationContextRunner().withUserConfiguration(SolutionConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(OrderService.class).channel()).isEqualTo("SMS");
        });
    }

    @Test
    @DisplayName("Wzorzec 3: analizator opisuje błąd z SKU i daje radę; cudze wyjątki ignoruje")
    void solution3() {
        SolutionStockFailureAnalyzer analyzer = new SolutionStockFailureAnalyzer();

        FailureAnalysis analysis = analyzer.analyze(
                new IllegalStateException("Start aplikacji nie powiódł się", new OutOfStockException("ELE-002")));
        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("ELE-002");
        assertThat(analysis.getAction()).isNotBlank();
        assertThat(analysis.getCause()).isInstanceOf(OutOfStockException.class);

        assertThat(analyzer.analyze(new IllegalStateException("zupełnie inny błąd"))).isNull();
    }
}
