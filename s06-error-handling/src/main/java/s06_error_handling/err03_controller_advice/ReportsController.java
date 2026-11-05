package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler raportów — oznaczony {@link ReportsApi}, więc obsługuje go {@code ReportsAdvice} (selektor {@code annotations}). */
@ReportsApi
@RestController
@RequestMapping("/err03/reports")
public class ReportsController {

    @GetMapping("/sales")
    public String sales() {
        throw new IllegalArgumentException("Zły zakres dat raportu");
    }
}
