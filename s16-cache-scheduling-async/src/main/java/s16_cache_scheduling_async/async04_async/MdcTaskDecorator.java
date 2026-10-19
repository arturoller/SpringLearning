package s16_cache_scheduling_async.async04_async;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * TaskDecorator (dekorator zadania) = „opakowanie”, które Spring nakłada na każde zadanie wysyłane do puli. Wykonuje
 * się w wątku WYSYŁAJĄCYM, więc może skopiować jego kontekst (MDC = diagnostic context logowania, mapa ThreadLocal
 * z np. identyfikatorem żądania) i ustawić go w wątku WYKONUJĄCYM. Po zadaniu przywraca poprzedni stan wątku puli.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> callerContext = MDC.getCopyOfContextMap(); // wątek wywołujący
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();  // wątek pracujący (może mieć stary kontekst)
            try {
                setOrClear(callerContext);
                task.run();
            } finally {
                setOrClear(previous);
            }
        };
    }

    private static void setOrClear(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
