package s01_core_ioc.solutions.ioc01_container;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import s01_core_ioc.ioc01_container.OrderCalculator;
import s01_core_ioc.ioc01_container.PriceList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ioc01Container — zajrzyj dopiero po własnej próbie! */
public final class Ioc01ContainerSolutions {

    private Ioc01ContainerSolutions() {
    }

    static int solution1(ApplicationContext context) {
        OrderCalculator calculator = context.getBean(OrderCalculator.class);
        return calculator.total(Map.of("Kawa", 2, "Herbata", 1));
    }

    static int solution2() {
        try (var context = new AnnotationConfigApplicationContext(PriceList.class, OrderCalculator.class)) {
            return context.getBean(OrderCalculator.class).total(Map.of("Czekolada", 3));
        }
    }

    static List<String> solution3(ApplicationContext context) {
        String lessonPackage = "s01_core_ioc.ioc01_container";
        return Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> {
                    Class<?> type = context.getType(name);
                    return type != null && type.getPackageName().equals(lessonPackage);
                })
                .sorted()
                .toList();
    }
}
