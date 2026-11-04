package s11_testing.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 1 lekcji Test07DockerCompose — PUŁAPKA: ścieżka do pliku compose jest WZGLĘDNA wobec katalogu roboczego.
 * Gdy pliku tam nie ma, aplikacja nie wstaje: Boot sprawdza plik, zanim w ogóle zawoła Dockera — więc ten test nie
 * potrzebuje Dockera. Uruchamiamy aplikację „ręcznie” ({@code SpringApplicationBuilder}) z
 * {@code skip.in-tests=false} — bez tego Boot wykryłby, że działa w teście (JUnit na stosie wywołań), i pominąłby compose
 * także tutaj (sprawdzone: start się wtedy UDAJE, na H2).
 */
class Test07MissingFileTest {

    @Test
    @DisplayName("Sekcja 1, PUŁAPKA: spring.docker.compose.file wskazuje nieistniejący plik → start kończy się błędem \"must exist\"")
    void missingComposeFile() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(Test07Application.class)
                .profiles("test07")
                .web(WebApplicationType.NONE)
                .properties("spring.docker.compose.file=src/compose-test07.yaml",  // zły katalog — pliku tam nie ma
                        "spring.docker.compose.skip.in-tests=false");

        assertThatThrownBy(app::run)
                .hasMessageContaining("compose-test07.yaml")
                .hasMessageContaining("must exist");
    }
}
