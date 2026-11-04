package s11_testing.solutions.test02_mockito_beans;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Konfiguracja testów rozwiązań Test02 (testy szukają {@code @SpringBootConfiguration} w górę pakietów). Leży
 * w pakiecie solutions.test02_mockito_beans (rodzeństwo lekcji), więc skanuje tylko ten pakiet — beany lekcji
 * testy dokładają przez {@code @Import}.
 */
@SpringBootApplication
public class Test02SolutionsApplication {
}
