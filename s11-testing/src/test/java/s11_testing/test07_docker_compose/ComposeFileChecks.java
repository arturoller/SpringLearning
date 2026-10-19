package s11_testing.test07_docker_compose;

import org.springframework.boot.docker.compose.core.DockerComposeFile;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pomocnik ćwiczenia 3 lekcji Test07DockerCompose: sprawdza plik Compose w katalogu BEZ uruchamiania Dockera —
 * czy Boot go znajdzie, czy obraz ma jawny tag i czy port nie jest przypięty do portu hosta. YAML czyta biblioteka
 * SnakeYAML (przychodzi ze starterem Spring Boota).
 */
public final class ComposeFileChecks {

    private ComposeFileChecks() {
    }

    @SuppressWarnings("unchecked")
    public static void assertGoodPostgresComposeFile(File directory) throws IOException {
        DockerComposeFile found = DockerComposeFile.find(directory);
        assertThat(found).as("Boot nie znalazł pliku Compose w %s — zła nazwa pliku?", directory).isNotNull();

        Map<String, Object> compose;
        try (Reader reader = Files.newBufferedReader(found.getFiles().getFirst().toPath(), StandardCharsets.UTF_8)) {
            compose = new Yaml().load(reader);
        }
        var postgres = (Map<String, Object>) ((Map<String, Object>) compose.get("services")).get("postgres");
        assertThat(postgres).as("brak usługi postgres").isNotNull();

        String image = (String) postgres.get("image");
        assertThat(image).as("obraz musi mieć jawny tag wersji").contains(":").doesNotEndWith(":latest");

        var ports = (List<Object>) postgres.get("ports");
        assertThat(ports).as("port kontenera 5432 bez portu hosta").containsExactly("5432");
    }
}
