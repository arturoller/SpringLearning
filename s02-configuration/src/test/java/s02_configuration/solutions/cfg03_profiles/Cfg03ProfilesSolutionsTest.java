package s02_configuration.solutions.cfg03_profiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import s02_configuration.cfg03_profiles.AuditLog;
import s02_configuration.cfg03_profiles.Cfg03Application;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Cfg03ProfilesExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Ćwiczenie 3 uruchamia Cfg03Application wprost przez SpringApplicationBuilder (nie potrzeba @SpringBootTest).
 */
@Tag("wzorzec")
class Cfg03ProfilesSolutionsTest {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withUserConfiguration(
                Cfg03ProfilesSolutions.ConsoleMailSender.class, Cfg03ProfilesSolutions.SmtpMailSender.class);
    }

    @Test
    @DisplayName("Wzorzec 1: bez profilu poczta idzie na konsolę, w cfg03-prod przez SMTP")
    void solution1() {
        runner().run(context -> assertThat(context.getBean(Cfg03ProfilesSolutions.MailSender.class))
                .isInstanceOf(Cfg03ProfilesSolutions.ConsoleMailSender.class));
        runner().withPropertyValues("spring.profiles.active=cfg03-prod").run(context ->
                assertThat(context.getBean(Cfg03ProfilesSolutions.MailSender.class))
                        .isInstanceOf(Cfg03ProfilesSolutions.SmtpMailSender.class));
    }

    @Test
    @DisplayName("Wzorzec 2: nazwa trybu z aktywnych profili")
    void solution2() {
        MockEnvironment dev = new MockEnvironment();
        assertThat(Cfg03ProfilesSolutions.solution2(dev)).isEqualTo("ROZWÓJ");

        MockEnvironment test = new MockEnvironment();
        test.setActiveProfiles("cfg03-test");
        assertThat(Cfg03ProfilesSolutions.solution2(test)).isEqualTo("TEST");

        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("cfg03-test", "cfg03-prod");
        assertThat(Cfg03ProfilesSolutions.solution2(prod)).isEqualTo("PRODUKCJA");
    }

    @Test
    @DisplayName("Wzorzec 3: grupa profili cfg03-full włącza cfg03-dev i cfg03-audit")
    void solution3() {
        List<String> args = new ArrayList<>();
        for (String setting : Cfg03ProfilesSolutions.solution3()) {
            args.add("--" + setting);
        }
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Cfg03Application.class)
                .web(WebApplicationType.NONE).run(args.toArray(new String[0]))) {
            assertThat(context.getEnvironment().getActiveProfiles()).contains("cfg03", "cfg03-dev", "cfg03-audit");
            assertThat(context.getBeansOfType(AuditLog.class)).hasSize(1);
        }
    }
}
