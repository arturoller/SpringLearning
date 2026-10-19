package s20_modulith.solutions.mod02_module_events;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import s20_modulith.mod02_module_events.orders.OrderPlaced;

import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń z lekcji Mod02ModuleEvents. Pakiet {@code solutions} leży poza pakietem głównym
 * {@code s20_modulith.mod02_module_events}, więc nie jest modułem tej aplikacji.
 */
public final class Mod02ModuleEventsSolutions {

    private Mod02ModuleEventsSolutions() {
    }

    /** ĆWICZENIE 1: liczba nieukończonych publikacji. */
    public static int solution1(JdbcClient jdbc) {
        return jdbc.sql("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL")
                .query(Integer.class)
                .single();
    }

    /** ĆWICZENIE 2: proste nazwy typów zdarzeń, których słucha moduł, alfabetycznie. */
    public static List<String> solution2(ApplicationModules modules, String moduleName) {
        ApplicationModule module = modules.getModuleByName(moduleName)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma modułu: " + moduleName));
        return module.getEventsListenedTo(modules).stream()
                .map(type -> type.getSimpleName())
                .distinct()
                .sorted()
                .toList();
    }

    /** ĆWICZENIE 3: ponowienie tylko publikacji OrderPlaced o podanym numerze zamówienia. */
    public static void solution3(IncompleteEventPublications incomplete, String orderId) {
        incomplete.resubmitIncompletePublications(publication ->
                publication.getEvent() instanceof OrderPlaced event && event.orderId().equals(orderId));
    }

    /** ĆWICZENIE 4: usunięcie wszystkich ukończonych publikacji; wynik = ile ich było. */
    public static int solution4(CompletedEventPublications completed) {
        int count = completed.findAll().size();
        completed.deletePublications(publication -> true);
        return count;
    }
}
