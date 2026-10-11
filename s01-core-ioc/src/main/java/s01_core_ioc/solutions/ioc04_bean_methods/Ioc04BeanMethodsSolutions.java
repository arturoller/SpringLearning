package s01_core_ioc.solutions.ioc04_bean_methods;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ioc04BeanMethods — zajrzyj dopiero po własnej próbie! */
public final class Ioc04BeanMethodsSolutions {

    private Ioc04BeanMethodsSolutions() {
    }

    static String solution1(Clock clock) {
        return "FV/" + LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyy/MM"));
    }
}
