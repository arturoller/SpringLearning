package s11_testing.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.docker.compose.core.DockerComposeFile;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 2 lekcji Test07DockerCompose: skąd Boot bierze plik Compose. Bez właściwości {@code spring.docker.compose.file}
 * szuka w KATALOGU ROBOCZYM plików o nazwach compose.yaml, compose.yml, docker-compose.yaml, docker-compose.yml
 * ({@code DockerComposeFile.find}). Testy biegną w katalogu modułu s11-testing (tak ustawia je Maven i IntelliJ).
 */
class Test07DockerComposeFileTest {

    @Test
    @DisplayName("2. W katalogu modułu NIE MA compose.yaml — bez spring.docker.compose.file Boot nic by nie znalazł")
    void noComposeFileInModuleDirectory() {
        assertThat(DockerComposeFile.find(new File("."))).isNull();
    }

    @Test
    @DisplayName("2. W katalogu compose/test07 Boot znajduje plik lekcji po standardowej nazwie compose.yaml")
    void lessonDirectoryHasStandardFileName() {
        DockerComposeFile found = DockerComposeFile.find(new File("compose/test07"));

        assertThat(found).isNotNull();
        assertThat(found.getFiles()).singleElement()
                .satisfies(file -> assertThat(file.getName()).isEqualTo("compose.yaml"));
    }
}
