package s11_testing.solutions.test03_mock_mvc_tester;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Konfiguracja testów rozwiązań Test03 (wycinek {@code @WebMvcTest} szuka {@code @SpringBootConfiguration} w górę
 * pakietów). Leży w pakiecie solutions.test03_mock_mvc_tester (rodzeństwo lekcji), więc skanuje tylko ten pakiet —
 * kontroler, serwis i advice lekcji test dokłada przez {@code @Import}.
 */
@SpringBootApplication
public class Test03SolutionsApplication {
}
