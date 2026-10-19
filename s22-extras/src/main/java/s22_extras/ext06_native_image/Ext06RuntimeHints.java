package s22_extras.ext06_native_image;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Podpowiedzi dla obrazu natywnego (lekcja Ext06NativeImage, sekcja 3). RuntimeHintsRegistrar (rejestrator
 * podpowiedzi czasu działania) jest wołany podczas przetwarzania AOT — Spring zapisuje podpowiedzi w plikach
 * META-INF/native-image/..., a kompilator GraalVM dołącza wskazane klasy i zasoby do programu.
 * Podpięty adnotacją {@code @ImportRuntimeHints(Ext06RuntimeHints.class)} w Ext06Application.
 */
public class Ext06RuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 1. Zasób: plik z nazwami klas promocji
        hints.resources().registerPattern(PromotionLoader.PROMOTIONS_FILE);

        // 2. Refleksja: klasy z pliku — Class.forName + wywołanie konstruktora
        hints.reflection()
                .registerType(PercentDiscountRule.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
                .registerType(FixedDiscountRule.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }
}
