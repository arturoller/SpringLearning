package s11_testing.solutions.test07_docker_compose;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.TimeUnit;

/**
 * Rozwiązanie ćwiczenia 5 lekcji Test07DockerCompose: warunek JUnit (execution condition = warunek wykonania) —
 * test biegnie tylko wtedy, gdy na komputerze działa polecenie {@code docker compose version}. To sprawdza KLIENTA
 * (program {@code docker} z wtyczką compose), a nie serwer Dockera — do tego służy {@code @EnabledIfDockerAvailable}.
 */
public class DockerComposeCliCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            Process process = new ProcessBuilder("docker", "compose", "version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return ConditionEvaluationResult.enabled("docker compose jest dostępny");
            }
            process.destroyForcibly();
            return ConditionEvaluationResult.disabled("docker compose nie odpowiada poprawnie");
        } catch (Exception e) { // brak programu docker → IOException
            return ConditionEvaluationResult.disabled("brak polecenia docker: " + e.getMessage());
        }
    }
}
