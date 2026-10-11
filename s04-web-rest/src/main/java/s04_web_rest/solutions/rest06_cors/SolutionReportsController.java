package s04_web_rest.solutions.rest06_cors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Kopia ReportsController z lekcji Rest06Cors — potrzebna, bo aplikacja rozwiązań skanuje tylko swój pakiet. */
@RestController
@RequestMapping("/api/exercise/reports")
public class SolutionReportsController {

    @GetMapping("/{id}")
    public Map<String, Object> report(@PathVariable long id) {
        return Map.of("id", id, "title", "Sprzedaż dzienna");
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @RequestBody Map<String, String> body) {
        return Map.of("id", id, "title", body.getOrDefault("title", "?"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }
}
