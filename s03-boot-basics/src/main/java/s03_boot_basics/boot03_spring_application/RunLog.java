package s03_boot_basics.boot03_spring_application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RunLog = dziennik uruchomienia. Runnery i nasłuchiwacz zdarzeń dopisują tu, co się stało i w jakiej kolejności,
 * a testy czytają ten dziennik zamiast logów z konsoli (deterministycznie, bez parsowania tekstu).
 */
@Component
public class RunLog {

    private final List<String> entries = new ArrayList<>();

    public synchronized void add(String entry) {
        entries.add(entry);
    }

    /** Kopia wpisów w kolejności dopisywania (niezmienna lista). */
    public synchronized List<String> entries() {
        return List.copyOf(entries);
    }
}
