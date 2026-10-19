package s16_cache_scheduling_async.async04_async;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Punkty końcowe (endpoints) do ręcznego sprawdzenia @Async z pliku .http. */
@RestController
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    /**
     * Zwraca CompletableFuture — Spring MVC zwalnia wątek żądania i dokończy odpowiedź, gdy future się zakończy
     * (obsługa asynchroniczna Servletu). Odpowiedź mówi, który wątek zbudował raport i który obsłużył żądanie.
     */
    @GetMapping("/api/reports/{name}")
    public CompletableFuture<Map<String, String>> report(@PathVariable String name) {
        String requestThread = Thread.currentThread().getName();
        return reports.buildReport(name).thenApply(worker -> Map.of("report", name, "builtBy", worker, "requestThread", requestThread));
    }

    /** Ta sama praca BEZ @Async — wykonuje się w wątku żądania. */
    @GetMapping("/api/reports/{name}/sync")
    public Map<String, String> reportSync(@PathVariable String name) throws Exception {
        return Map.of("report", name, "builtBy", reports.callFromInside(), "requestThread", Thread.currentThread().getName());
    }
}
