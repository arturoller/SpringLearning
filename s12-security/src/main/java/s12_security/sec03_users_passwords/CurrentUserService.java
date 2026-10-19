package s12_security.sec03_users_passwords;

import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CurrentUserService = „kto jest zalogowany?” z głębi aplikacji, bez parametrów z kontrolera (sekcja 5 lekcji).
 *
 * <p>{@code SecurityContextHolder} (przechowywacz kontekstu bezpieczeństwa) trzyma dane zalogowanego w zmiennej
 * WĄTKU (ThreadLocal — zmienna lokalna wątku). Filtr Security wkłada je tam na początku zapytania, a FilterChainProxy
 * czyści po zapytaniu. Każdy kod wykonywany w TYM SAMYM wątku je widzi — kod w innym wątku już nie.</p>
 */
@Service
public class CurrentUserService {

    /** Login zalogowanego albo „nikt”, gdy w wątku nie ma uwierzytelnienia. */
    public String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "nikt" : authentication.getName();
    }

    /**
     * Ten sam odczyt w trzech miejscach: w bieżącym wątku, w NOWYM wątku i w nowym wątku z zadaniem opakowanym
     * w {@code DelegatingSecurityContextRunnable} (zadanie, które przenosi kontekst do innego wątku).
     */
    public Map<String, String> whoIsLoggedInWhere() throws InterruptedException {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("sameThread", currentUsername());

        AtomicReference<String> plain = new AtomicReference<>();
        Thread plainThread = new Thread(() -> plain.set(currentUsername()));
        plainThread.start();
        plainThread.join();   // join = poczekaj, aż wątek skończy
        result.put("newThread", plain.get());

        AtomicReference<String> delegating = new AtomicReference<>();
        Thread delegatingThread = new Thread(new DelegatingSecurityContextRunnable(() -> delegating.set(currentUsername())));
        delegatingThread.start();
        delegatingThread.join();
        result.put("newThreadWithDelegatingRunnable", delegating.get());
        return result;
    }
}
