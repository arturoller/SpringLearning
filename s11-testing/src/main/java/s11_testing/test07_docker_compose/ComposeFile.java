package s11_testing.test07_docker_compose;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Szuka pliku {@code compose-test07.yaml} względem KATALOGU ROBOCZEGO (working directory) — bo tak samo robi Boot:
 * {@code spring.docker.compose.file} to zwykły {@code java.io.File}, a ścieżka względna liczy się od katalogu, z którego
 * uruchomiono JVM. Ten katalog bywa różny:
 * <ul>
 *   <li>{@code ./mvnw -pl s11-testing spring-boot:run} i testy w Maven (surefire) — katalog MODUŁU {@code s11-testing},</li>
 *   <li>IntelliJ — zależnie od konfiguracji uruchomienia (Run Configuration → Working directory): katalog modułu
 *       ({@code $MODULE_WORKING_DIR$}) albo katalog główny projektu.</li>
 * </ul>
 * Dlatego sprawdzamy oba miejsca i zwracamy ścieżkę, która istnieje.
 */
public final class ComposeFile {

    static final String NAME = "compose-test07.yaml";

    private ComposeFile() {
    }

    /** Ścieżka do pliku compose lekcji: najpierw w katalogu roboczym, potem w podkatalogu s11-testing. */
    public static String locate() {
        if (Files.isRegularFile(Path.of(NAME))) {
            return NAME;                              // katalog roboczy = s11-testing
        }
        Path fromProjectRoot = Path.of("s11-testing", NAME);
        if (Files.isRegularFile(fromProjectRoot)) {
            return fromProjectRoot.toString();        // katalog roboczy = katalog główny projektu
        }
        return NAME;                                  // nie ma — Boot zgłosi czytelny błąd "'file' [...] must exist"
    }
}
