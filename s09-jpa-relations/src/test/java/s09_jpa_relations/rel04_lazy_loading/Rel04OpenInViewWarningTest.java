package s09_jpa_relations.rel04_lazy_loading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test sekcji 5 lekcji Rel04LazyLoading: ostrzeżenie o open-in-view w logu STARTU aplikacji.
 *
 * <p>Zwykły test JUnita (bez {@code @SpringBootTest}) — aplikację lekcji uruchamiamy sami przez
 * SpringApplicationBuilder (budowniczy aplikacji), żeby log startu na pewno trafił do przechwyconego wyjścia
 * ({@code OutputCaptureExtension} — rozszerzenie JUnita z Spring Boota, przechwytuje System.out). Prawdziwy Tomcat
 * startuje na losowym wolnym porcie ({@code server.port=0}), baza ma własną nazwę, żeby nie dzielić danych
 * z innymi testami.</p>
 */
@ExtendWith(OutputCaptureExtension.class)
class Rel04OpenInViewWarningTest {

    private static final String WARNING = "spring.jpa.open-in-view is enabled by default. Therefore, database queries "
            + "may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning";

    @Test
    @DisplayName("5. Bez ustawienia spring.jpa.open-in-view Boot loguje ostrzeżenie (WARN) przy starcie")
    void defaultLogsWarning(CapturedOutput output) {
        try (ConfigurableApplicationContext ignored = start()) {
            assertThat(output).contains(WARNING);
            assertThat(output.getOut().lines().filter(line -> line.contains(WARNING)).findFirst().orElseThrow())
                    .contains(" WARN ");
        }
    }

    @Test
    @DisplayName("5. Jawne spring.jpa.open-in-view=false — ostrzeżenia nie ma")
    void explicitSettingSilencesWarning(CapturedOutput output) {
        try (ConfigurableApplicationContext ignored = start("spring.jpa.open-in-view=false")) {
            assertThat(output).doesNotContain(WARNING);
        }
    }

    private static ConfigurableApplicationContext start(String... extraProperties) {
        return new SpringApplicationBuilder(Rel04Application.class)
                .profiles("rel04")
                .properties("server.port=0", "spring.datasource.url=jdbc:h2:mem:rel04-warning",
                        "spring.jpa.show-sql=false")
                .properties(extraProperties)
                .run();
    }
}
