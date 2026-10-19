package s15_actuator_logging.solutions.obs03_metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s15_actuator_logging.solutions.obs03_metrics.Obs03MetricsSolutions.TagCounters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Obs03MetricsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Obs03MetricsSolutionsTest {

    @Test
    @DisplayName("Wzorzec 1: nazwa metryki małymi literami z kropką")
    void solution1() {
        assertThat(Obs03MetricsSolutions.solution1("Orders", " Created ")).isEqualTo("orders.created");
        assertThat(Obs03MetricsSolutions.solution1("http", "SERVER")).isEqualTo("http.server");
        assertThatThrownBy(() -> Obs03MetricsSolutions.solution1("  ", "created"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Obs03MetricsSolutions.solution1("orders", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: serie rozróżniane po nazwie i tagach (kolejność tagów bez znaczenia)")
    void solution2() {
        TagCounters counters = new TagCounters();
        counters.increment("orders.created", Map.of("channel", "WEB"));
        counters.increment("orders.created", Map.of("channel", "WEB"));
        counters.increment("orders.created", Map.of("channel", "SKLEP"));

        assertThat(counters.count("orders.created", Map.of("channel", "WEB"))).isEqualTo(2);
        assertThat(counters.count("orders.created", Map.of("channel", "SKLEP"))).isEqualTo(1);
        assertThat(counters.count("orders.created", Map.of("channel", "INNY"))).isZero();
        assertThat(counters.seriesCount("orders.created")).isEqualTo(2);
        assertThat(counters.seriesCount("nie.ma")).isZero();

        Map<String, String> ab = new LinkedHashMap<>();
        ab.put("a", "1");
        ab.put("b", "2");
        Map<String, String> ba = new LinkedHashMap<>();
        ba.put("b", "2");
        ba.put("a", "1");
        counters.increment("x", ab);
        counters.increment("x", ba);
        assertThat(counters.count("x", ab)).isEqualTo(2);
        assertThat(counters.seriesCount("x")).isEqualTo(1);
    }

    @Test
    @DisplayName("Wzorzec 3: tagi o zbyt wysokiej kardynalności")
    void solution3() {
        Map<String, Set<String>> observed = Map.of(
                "channel", Set.of("WEB", "SKLEP"),
                "customerId", Set.of("1", "2", "3", "4", "5", "6"),
                "email", Set.of("a", "b", "c", "d", "e", "f", "g"));

        assertThat(Obs03MetricsSolutions.solution3(observed, 5)).containsExactly("customerId", "email");
        assertThat(Obs03MetricsSolutions.solution3(observed, 6)).containsExactly("email");
        assertThat(Obs03MetricsSolutions.solution3(observed, 100)).isEmpty();
        assertThatThrownBy(() -> Obs03MetricsSolutions.solution3(observed, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
