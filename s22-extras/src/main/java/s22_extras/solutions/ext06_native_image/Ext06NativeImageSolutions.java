package s22_extras.solutions.ext06_native_image;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import s22_extras.ext06_native_image.PercentDiscountRule;
import s22_extras.ext06_native_image.PromotionRule;

import java.math.BigDecimal;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ext06NativeImage — zajrzyj dopiero po własnej próbie! */
public final class Ext06NativeImageSolutions {

    private Ext06NativeImageSolutions() {
    }

    static void solution1(RuntimeHints hints) {
        hints.resources().registerPattern("ext06/regulamin-*.txt");                              // * = dowolny ciąg
        hints.reflection().registerType(PercentDiscountRule.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    static RuntimeHintsRegistrar solution2(List<String> classNames) {
        return (hints, classLoader) -> classNames.forEach(name ->
                hints.reflection().registerTypeIfPresent(classLoader, name, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
    }

    static void solution3(RuntimeHints hints) {
        hints.proxies().registerJdkProxy(PromotionRule.class);
        try {
            hints.reflection().registerMethod(PromotionRule.class.getMethod("apply", BigDecimal.class), ExecutableMode.INVOKE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("PromotionRule nie ma metody apply(BigDecimal)", e);
        }
    }
}
