package s04_web_rest.rest01_controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sekcja 4 lekcji: {@code produces} (co ODDAJEMY — porównywane z nagłówkiem Accept zapytania) i {@code consumes}
 * (co PRZYJMUJEMY — porównywane z nagłówkiem Content-Type zapytania).
 * Dwie metody GET pod tą samą ścieżką są dozwolone, bo różnią się {@code produces}: wybór robi nagłówek Accept.
 */
@RestController
@RequestMapping("/rest01/report")
public class ReportController {

    @GetMapping(produces = "text/csv")
    public String csv() {
        return "id;name\n1;Kawa\n2;Herbata\n";
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Line> json() {
        return List.of(new Line(1, "Kawa"), new Line(2, "Herbata"));
    }

    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String importJson() {
        return "przyjęto JSON";
    }

    public record Line(int id, String name) {
    }
}
