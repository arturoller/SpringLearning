package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Test05TestConfiguration: {@code ApplicationContextRunner} (runner = uruchamiacz) — mały kontekst
 * z WYBRANĄ konfiguracją, budowany w milisekundach, bez skanowania i bez autokonfiguracji. Idealny do sprawdzania
 * warunków {@code @ConditionalOnProperty} (s03_boot_basics/Boot01AutoConfiguration). Nie potrzebuje
 * {@code @SpringBootTest} — to zwykły test JUnit.
 */
class Test05ContextRunnerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(NewsletterConfig.class); // user configuration = konfiguracja użytkownika

    @Test
    @DisplayName("Sekcja 5: bez właściwości test05.newsletter.enabled → jedyny NewsletterSender to wersja 'nic nie rób'")
    void noPropertyMeansNoop() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(NewsletterSender.class);
            assertThat(context).hasBean("noopNewsletterSender").doesNotHaveBean("emailNewsletterSender");
        });
    }

    @Test
    @DisplayName("Sekcja 5: test05.newsletter.enabled=true → wysyłka e-mailem")
    void enabledMeansEmail() {
        runner.withPropertyValues("test05.newsletter.enabled=true")
                .run(context -> assertThat(context.getBean(NewsletterSender.class).send("Black Week"))
                        .isEqualTo("E-mail: Black Week"));
    }
}
