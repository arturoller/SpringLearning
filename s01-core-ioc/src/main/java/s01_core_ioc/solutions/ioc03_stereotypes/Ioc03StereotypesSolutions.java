package s01_core_ioc.solutions.ioc03_stereotypes;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ioc03Stereotypes — zajrzyj dopiero po własnej próbie! */
public final class Ioc03StereotypesSolutions {

    private Ioc03StereotypesSolutions() {
    }

    static String solution1(String simpleClassName) {
        if (simpleClassName.isEmpty()) {
            return simpleClassName;
        }
        if (simpleClassName.length() > 1
                && Character.isUpperCase(simpleClassName.charAt(0))
                && Character.isUpperCase(simpleClassName.charAt(1))) {
            return simpleClassName;
        }
        return Character.toLowerCase(simpleClassName.charAt(0)) + simpleClassName.substring(1);
    }

    static boolean solution2(Class<?> type) {
        return AnnotatedElementUtils.hasAnnotation(type, Component.class);
    }

    static List<String> solution3(ApplicationContext context) {
        return Arrays.stream(context.getBeanNamesForAnnotation(Service.class)).sorted().toList();
    }
}
