package s22_extras.ext06_native_image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lekcja Ext06NativeImage, sekcja 3: test rejestratora podpowiedzi BEZ GraalVM i bez Springa. RuntimeHintsPredicates
 * (predykaty podpowiedzi) odpowiadają na pytanie: „czy obraz natywny będzie miał to, czego kod użyje?”.
 */
class Ext06NativeImageTest {

    static RuntimeHints registeredHints() {
        RuntimeHints hints = new RuntimeHints();
        new Ext06RuntimeHints().registerHints(hints, Ext06NativeImageTest.class.getClassLoader());
        return hints;
    }

    @Test
    @DisplayName("3. Rejestrator zgłasza plik ext06/promocje.txt jako zasób")
    void resourceHint() {
        assertThat(RuntimeHintsPredicates.resource().forResource("ext06/promocje.txt")).accepts(registeredHints());
        assertThat(RuntimeHintsPredicates.resource().forResource("ext06/inny-plik.txt")).rejects(registeredHints());
    }

    @Test
    @DisplayName("3. Rejestrator zgłasza klasy promocji z wywołaniem konstruktorów (Class.forName + newInstance)")
    void reflectionHints() {
        RuntimeHints hints = registeredHints();

        assertThat(RuntimeHintsPredicates.reflection().onType(PercentDiscountRule.class)
                .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(FixedDiscountRule.class)
                .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
    }

    @Test
    @DisplayName("3. PUŁAPKA: podpowiedź obejmuje TYLKO to, co zgłoszono — klasa spoza listy i metody refleksją nie przejdą")
    void hintsAreExact() {
        RuntimeHints hints = registeredHints();

        assertThat(RuntimeHintsPredicates.reflection().onType(PromotionLoader.class)).rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(PercentDiscountRule.class, "apply"))
                .as("konstruktor tak, ale wywołanie apply() przez refleksję — nie").rejects(hints);
    }

    @Test
    @DisplayName("3. Na JVM refleksja działa bez żadnych podpowiedzi: PromotionLoader tworzy obie promocje z pliku")
    void reflectionWorksOnJvm() {
        assertThat(new PromotionLoader().loadPromotions())
                .extracting(rule -> rule.apply(new BigDecimal("349.90")))
                .containsExactly(new BigDecimal("314.91"), new BigDecimal("329.90"));
        assertThatThrownBy(() -> PromotionLoader.instantiate("s22_extras.ext06_native_image.BrakTakiejKlasy"))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(ClassNotFoundException.class);
    }
}
