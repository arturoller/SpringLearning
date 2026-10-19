package s22_extras.ext04_tracing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s22_extras.ext04_tracing.Ext04Tracing.SpanInfo;
import s22_extras.ext04_tracing.Ext04Tracing.TraceParent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Ext04Tracing. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Ext04TracingExercisesTest {

    static final String TRACE = "8107e1277d9c0705af5c4f03ac7b198b";
    static final String SPAN = "57f40112a07e7c4d";

    @Test
    @DisplayName("Ćwiczenie 1: rozbiór nagłówka traceparent (flagi 03 i 01 = zapisz, 02 i 00 = nie)")
    void exercise1() {
        assertThat(Ext04Tracing.exercise1("00-" + TRACE + "-" + SPAN + "-03"))
                .isEqualTo(new TraceParent("00", TRACE, SPAN, true));
        assertThat(Ext04Tracing.exercise1("00-" + TRACE + "-" + SPAN + "-01").sampled()).isTrue();
        assertThat(Ext04Tracing.exercise1("00-" + TRACE + "-" + SPAN + "-02").sampled()).isFalse();
        assertThat(Ext04Tracing.exercise1("00-" + TRACE + "-" + SPAN + "-00").sampled()).isFalse();
        assertThatThrownBy(() -> Ext04Tracing.exercise1("00-" + TRACE + "-" + SPAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ext04Tracing.exercise1("00-" + TRACE.toUpperCase() + "-" + SPAN + "-01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ext04Tracing.exercise1("00-abc-" + SPAN + "-01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: budowa nagłówka traceparent i odrzucanie złych identyfikatorów")
    void exercise2() {
        assertThat(Ext04Tracing.exercise2(TRACE, SPAN, true)).isEqualTo("00-" + TRACE + "-" + SPAN + "-01");
        assertThat(Ext04Tracing.exercise2(TRACE, SPAN, false)).isEqualTo("00-" + TRACE + "-" + SPAN + "-00");
        assertThatThrownBy(() -> Ext04Tracing.exercise2("0".repeat(32), SPAN, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ext04Tracing.exercise2(TRACE, "0".repeat(16), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Ext04Tracing.exercise2(TRACE, "xyz", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 3: drzewo odcinków z listy w kolejności kończenia (dzieci przed rodzicami)")
    void exercise3() {
        List<SpanInfo> spans = List.of(   // kolejność jak z eksportera: najpierw kończą się dzieci
                new SpanInfo("http get /api/stock/{sku}", "d4", "c3"),
                new SpanInfo("http get", "c3", "b2"),
                new SpanInfo("sprawdz-dostepnosc", "b2", "a1"),
                new SpanInfo("authorize request", "e5", "a1"),
                new SpanInfo("http get /api/shop/{sku}/availability", "a1", "0000000000000000"));

        assertThat(Ext04Tracing.exercise3(spans)).containsExactly(
                "http get /api/shop/{sku}/availability",
                "  authorize request",
                "  sprawdz-dostepnosc",
                "    http get",
                "      http get /api/stock/{sku}");
        assertThat(Ext04Tracing.exercise3(List.of())).isEmpty();
    }
}
