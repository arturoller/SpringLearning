package s01_core_ioc.solutions.ioc07_lifecycle;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import s01_core_ioc.ioc07_lifecycle.EventLog;
import s01_core_ioc.ioc07_lifecycle.ProductCatalog;
import s01_core_ioc.ioc07_lifecycle.WarehouseConnection;

import java.util.List;

/** Rozwiązanie ćwiczenia 3 lekcji Ioc07Lifecycle — zajrzyj dopiero po własnej próbie! */
public final class Ioc07LifecycleSolutions {

    private Ioc07LifecycleSolutions() {
    }

    static List<String> solution3() {
        EventLog log;
        try (var context = new AnnotationConfigApplicationContext(EventLog.class, ProductCatalog.class,
                WarehouseConnection.class)) {
            log = context.getBean(EventLog.class);
        } // tu kontekst jest już zamknięty, więc @PreDestroy się wykonał
        return log.events(); // events() zwraca kopię listy
    }
}
