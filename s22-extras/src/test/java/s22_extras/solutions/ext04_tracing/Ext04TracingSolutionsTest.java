package s22_extras.solutions.ext04_tracing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s22_extras.ext04_tracing.Ext04Tracing.SpanInfo;
import s22_extras.ext04_tracing.Ext04Tracing.TraceParent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Ext04TracingExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Ext04TracingSolutionsTest {

    static final String TRACE = "8107e1277d9c0705af5c4f03ac7b198b";
    static final String SPAN = "57f40112a07e7c4d";

    @Test
    @DisplayName("Wzorzec 1: rozbiór nagłówka traceparent (flagi 03 i 01 = zapisz, 02 i 00 = nie)")
    void solution1() {
        assertThat(Ext04TracingSolutions.solution1("00-" + TRACE + "-" + SPAN + "-03"))
                .isEqualTo(new TraceParent("00", TRACE, SPAN, true));
        assertThat(Ext04TracingSolutions.solution1("00-" + TRACE + "-" + SPAN + "-01").sampled()).isTrue();
        assertThat(Ext04TracingSolutions.solution1("00-" + TRACE + "-" + SPAN + "-02").sampled()).isFalse();
        assertThat(Ext04TracingSolutions.solution1("00-" + TRACE + "-" + SPAN + "-00").sampled()).isFalse();
        assertThatThrownBy(() -> Ext04TracingSolutions.solution1("00-" + TRACE + "-" + SPAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ext04TracingSolutions.solution1("00-" + TRACE.toUpperCase() + "-" + SPAN + "-01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ext04TracingSolutions.solution1("00-abc-" + SPAN + "-01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: budowa nagłówka traceparent i odrzucanie złych identyfikatorów")
    void solution2() {
        assertThat(Ext04TracingSolutions.solution2(TRACE, SPAN, true)).isEqualTo("00-" + TRACE + "-" + SPAN + "-01");
        assertThat(Ext04TracingSolutions.solution2(TRACE, SPAN, false)).isEqualTo("00-" + TRACE + "-" + SPAN + "-00");
        assertThatThrownBy(() -> Ext04TracingSolutions.solution2("0".repeat(32), SPAN, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ext04TracingSolutions.solution2(TRACE, "0".repeat(16), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ext04TracingSolutions.solution2(TRACE, "xyz", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 3: drzewo odcinków z listy w kolejności kończenia (dzieci przed rodzicami)")
    void solution3() {
        List<SpanInfo> spans = List.of(
                new SpanInfo("http get /api/stock/{sku}", "d4", "c3"),
                new SpanInfo("http get", "c3", "b2"),
                new SpanInfo("sprawdz-dostepnosc", "b2", "a1"),
                new SpanInfo("authorize request", "e5", "a1"),
                new SpanInfo("http get /api/shop/{sku}/availability", "a1", "0000000000000000"));

        assertThat(Ext04TracingSolutions.solution3(spans)).containsExactly(
                "http get /api/shop/{sku}/availability",
                "  authorize request",
                "  sprawdz-dostepnosc",
                "    http get",
                "      http get /api/stock/{sku}");
        assertThat(Ext04TracingSolutions.solution3(List.of())).isEmpty();
    }
}
