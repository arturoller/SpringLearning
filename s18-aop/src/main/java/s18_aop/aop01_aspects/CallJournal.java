package s18_aop.aop01_aspects;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dziennik wywołań (journal = dziennik): aspekty i metody zapisują tu, co się wydarzyło i w jakiej kolejności.
 * Testy czytają dziennik i sprawdzają kolejność — to jest „wynik” lekcji. Metody są synchronizowane
 * (synchronized = tylko jeden wątek naraz), żeby dziennik był bezpieczny dla wątków.
 * Sam dziennik NIE jest objęty żadnym aspektem, więc Spring go nie opakowuje w proxy (test sekcji 1).
 */
@Component
public class CallJournal {

    private final List<String> entries = new ArrayList<>();

    public synchronized void add(String entry) {
        entries.add(entry);
    }

    /** Kopia wpisów — test nie może przypadkiem zmienić dziennika. */
    public synchronized List<String> entries() {
        return List.copyOf(entries);
    }

    /** Wpisy zaczynające się od podanego przedrostka (prefix = przedrostek), np. „LOG” albo „PC”. */
    public synchronized List<String> entriesStartingWith(String prefix) {
        return entries.stream().filter(entry -> entry.startsWith(prefix)).toList();
    }

    public synchronized void clear() {
        entries.clear();
    }
}
