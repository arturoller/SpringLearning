package s11_testing.test07_docker_compose;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test07DockerCompose: profil test07 WŁĄCZA obsługę Docker Compose, a mimo to w teście Boot jej NIE
 * uruchamia (spring.docker.compose.skip.in-tests=true domyślnie). Kontekst startuje bez Dockera, a baza to H2
 * wbudowana — ten sam schemat i dane z plików SQL lekcji. Test przechodzi także na komputerze bez Dockera.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test07")
class Test07DockerComposeSkipInTestsTest {

    @Autowired
    Environment environment;

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("4. Compose włączony w profilu, ale w teście POMINIĘTY: baza to H2, dane z db/test07 wczytane")
    void composeIsSkippedInTests() {
        assertThat(environment.getProperty("spring.docker.compose.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("spring.docker.compose.file")).isEqualTo("compose/test07/compose.yaml");

        assertThat(repository.databaseVersion()).startsWith("H2");
        assertThat(repository.findAll()).extracting(Product::sku)
                .containsExactly("DOM-001", "DOM-002", "KSI-001", "KSI-002", "KSI-003");
    }
}
