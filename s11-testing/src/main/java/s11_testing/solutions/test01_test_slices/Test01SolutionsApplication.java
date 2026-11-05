package s11_testing.solutions.test01_test_slices;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Konfiguracja testów rozwiązań Test01 (wycinki szukają {@code @SpringBootConfiguration} w górę pakietów). Leży w
 * pakiecie solutions.test01_test_slices (rodzeństwo lekcji), więc skanuje tylko ten pakiet — kontroler i advice
 * lekcji testy dokładają przez {@code @Import}.
 */
@SpringBootApplication
public class Test01SolutionsApplication {
}
