package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import s06_error_handling.err03_controller_advice.Err03ControllerAdvice.AdviceDef;
import s06_error_handling.err03_controller_advice.Err03ControllerAdvice.FieldProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Err03ControllerAdvice. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Err03ControllerAdviceExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: status HTTP dla wyjątku")
    void exercise1() {
        assertThat(Err03ControllerAdvice.statusFor(new ProductNotFoundException("X"))).isEqualTo(404);
        assertThat(Err03ControllerAdvice.statusFor(new IllegalArgumentException())).isEqualTo(400);
        assertThat(Err03ControllerAdvice.statusFor(new UnsupportedOperationException())).isEqualTo(501);
        assertThat(Err03ControllerAdvice.statusFor(new IllegalStateException())).isEqualTo(500);
    }

    @Test
    @DisplayName("Ćwiczenie 2: lista błędów pól → posortowana mapa pole → komunikat")
    void exercise2() {
        Map<String, String> map = Err03ControllerAdvice.toFieldMap(List.of(
                new FieldProblem("sku", "za krótkie"), new FieldProblem("ilosc", "ujemna"), new FieldProblem("sku", "puste")));

        assertThat(map.keySet()).containsExactly("ilosc", "sku");
        assertThat(map).containsEntry("ilosc", "ujemna").containsEntry("sku", "puste; za krótkie");
        assertThat(Err03ControllerAdvice.toFieldMap(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 3: która porada odpowie — liczy się @Order, nie szczegółowość")
    void exercise3() {
        AdviceDef broad = new AdviceDef("szeroka", 1, Set.of(), Set.of(Exception.class));
        AdviceDef specific = new AdviceDef("szczegolowa", 2, Set.of(), Set.of(IllegalArgumentException.class));
        AdviceDef billing = new AdviceDef("billing", Integer.MIN_VALUE, Set.of(BillingController.class), Set.of(IllegalStateException.class));
        List<AdviceDef> advices = new ArrayList<>(List.of(specific, broad, billing));

        assertThat(Err03ControllerAdvice.whichAdviceAnswers(advices, ProductController.class, IllegalArgumentException.class))
                .isEqualTo("szeroka");
        assertThat(Err03ControllerAdvice.whichAdviceAnswers(advices, BillingController.class, IllegalStateException.class))
                .isEqualTo("billing");
        assertThat(Err03ControllerAdvice.whichAdviceAnswers(advices, ProductController.class, IllegalStateException.class))
                .isEqualTo("szeroka");
        assertThat(Err03ControllerAdvice.whichAdviceAnswers(List.of(specific), ProductController.class, IllegalStateException.class))
                .isNull();
        assertThat(advices).containsExactly(specific, broad, billing);   // wejściowa lista nie została zmieniona
    }
}
