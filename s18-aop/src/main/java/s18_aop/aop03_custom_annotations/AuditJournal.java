package s18_aop.aop03_custom_annotations;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dziennik audytu i pomiarów: aspekty i metody zapisują tu wpisy, a testy sprawdzają ich treść i kolejność.
 * Metody synchronizowane — dziennik jest bezpieczny dla wątków.
 */
@Component
public class AuditJournal {

    private final List<String> entries = new ArrayList<>();

    public synchronized void add(String entry) {
        entries.add(entry);
    }

    public synchronized List<String> entries() {
        return List.copyOf(entries);
    }

    public synchronized void clear() {
        entries.clear();
    }
}
