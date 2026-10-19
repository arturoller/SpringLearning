package s13_rest_client.rc04_resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.resilience.retry.MethodRetryEvent;
import org.springframework.stereotype.Component;

/**
 * RetryLogger = słuchacz zdarzeń ponawiania. Przy każdej nieudanej próbie metody z {@code @Retryable} Spring
 * Framework 7 publikuje MethodRetryEvent (zdarzenie ponowienia metody); gdy próby się skończą — jeszcze jedno,
 * z {@code isRetryAborted() == true}. Bez tego ponowienia są NIEWIDOCZNE — a warto wiedzieć, że dostawca „kaszle”.
 * Zdarzenia aplikacji — dział s17_events.
 */
@Component
public class RetryLogger {

    private static final Logger log = LoggerFactory.getLogger(RetryLogger.class);

    @EventListener
    public void onRetryEvent(MethodRetryEvent event) {
        if (event.isRetryAborted()) {
            log.error("Koniec prób dla {}: {}", event.getMethod().getName(), event.getFailure().toString());
        } else {
            log.warn("Nieudana próba {}: {}", event.getMethod().getName(), event.getFailure().toString());
        }
    }
}
