package s22_extras.ext06_native_image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Ext06NativeImage. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Ext06NativeImageExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: zasoby ext06/regulamin-*.txt i wywoływanie metod publicznych PercentDiscountRule")
    void exercise1() {
        RuntimeHints hints = new RuntimeHints();

        Ext06NativeImage.exercise1(hints);

        assertThat(RuntimeHintsPredicates.resource().forResource("ext06/regulamin-2026.txt")).accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("ext06/promocje.txt")).rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(PercentDiscountRule.class, "apply")).accepts(hints);
    }

    @Test
    @DisplayName("Ćwiczenie 2: rejestrator klas z listy — tylko istniejące, literówki pominięte bez wyjątku")
    void exercise2() {
        RuntimeHints hints = new RuntimeHints();

        Ext06NativeImage.exercise2(List.of(
                        "s22_extras.ext06_native_image.PercentDiscountRule",
                        "s22_extras.ext06_native_image.PercentDiscuntRule"))      // literówka
                .registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection().onType(PercentDiscountRule.class)
                .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TypeReference.of("s22_extras.ext06_native_image.PercentDiscuntRule"))).rejects(hints);
    }

    @Test
    @DisplayName("Ćwiczenie 3: proxy JDK dla PromotionRule i wywołanie apply() refleksją")
    void exercise3() {
        RuntimeHints hints = new RuntimeHints();

        Ext06NativeImage.exercise3(hints);

        assertThat(RuntimeHintsPredicates.proxies().forInterfaces(PromotionRule.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(PromotionRule.class, "apply")).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(PromotionRule.class, "description"))
                .as("tylko apply — description nie było zgłaszane").rejects(hints);
    }
}
