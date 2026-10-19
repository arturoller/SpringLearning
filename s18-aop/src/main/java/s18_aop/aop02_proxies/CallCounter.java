package s18_aop.aop02_proxies;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

/**
 * Licznik wywołań przechwyconych przez CountingAspect: nazwa metody → ile razy porada się wykonała.
 * Jeśli licznik metody stoi w miejscu, to znaczy, że wywołanie NIE przeszło przez proxy.
 * Nazwa beana „callCounter” nie pasuje do punktu przecięcia, więc sam licznik nie jest opakowany w proxy.
 */
@Component
public class CallCounter {

    private final Map<String, Integer> counts = new TreeMap<>();

    public synchronized void increment(String methodName) {
        counts.merge(methodName, 1, Integer::sum);
    }

    public synchronized int count(String methodName) {
        return counts.getOrDefault(methodName, 0);
    }

    public synchronized void reset() {
        counts.clear();
    }
}
