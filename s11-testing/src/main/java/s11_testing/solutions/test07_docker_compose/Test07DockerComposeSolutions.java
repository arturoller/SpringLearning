package s11_testing.solutions.test07_docker_compose;

import java.util.Map;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Test07DockerCompose — zajrzyj dopiero po własnej próbie!
 * Ćwiczenie 1 i 3: w teście Test07DockerComposeSolutionsTest i w pliku s11-testing/compose/solutions/exercise07/compose.yaml.
 */
public final class Test07DockerComposeSolutions {

    private Test07DockerComposeSolutions() {
    }

    /**
     * Rozwiązanie 2: klucze dokładnie takie jak w application.properties (zapis z myślnikami, kebab-case).
     * Map.of — niezmienna mapa; kolejność wpisów nie ma znaczenia.
     */
    public static Map<String, String> solution2() {
        return Map.of(
                "spring.docker.compose.enabled", "true",
                "spring.docker.compose.file", "compose/test07/compose.yaml",
                "spring.docker.compose.stop.command", "down",
                "spring.docker.compose.skip.in-tests", "false");
    }
}
