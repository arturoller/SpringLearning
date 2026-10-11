package s02_configuration.cfg03_profiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Cfg03Profiles. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Cfg03ProfilesExercisesTest {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(Cfg03Profiles.ConsoleMailSender.class, Cfg03Profiles.SmtpMailSender.class);
    }

    @Test
    @DisplayName("Ćwiczenie 1: bez profilu poczta idzie na konsolę, w cfg03-prod przez SMTP")
    void exercise1() {
        runner().run(context -> assertThat(context.getBean(Cfg03Profiles.MailSender.class))
                .isInstanceOf(Cfg03Profiles.ConsoleMailSender.class));
        runner().withPropertyValues("spring.profiles.active=cfg03-prod").run(context ->
                assertThat(context.getBean(Cfg03Profiles.MailSender.class)).isInstanceOf(Cfg03Profiles.SmtpMailSender.class));
    }

    @Test
    @DisplayName("Ćwiczenie 2: nazwa trybu z aktywnych profili")
    void exercise2() {
        MockEnvironment dev = new MockEnvironment();
        assertThat(Cfg03Profiles.exercise2(dev)).isEqualTo("ROZWÓJ");

        MockEnvironment test = new MockEnvironment();
        test.setActiveProfiles("cfg03-test");
        assertThat(Cfg03Profiles.exercise2(test)).isEqualTo("TEST");

        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("cfg03-test", "cfg03-prod");
        assertThat(Cfg03Profiles.exercise2(prod)).isEqualTo("PRODUKCJA");
    }

    @Test
    @DisplayName("Ćwiczenie 3: grupa profili cfg03-full włącza cfg03-dev i cfg03-audit")
    void exercise3() {
        List<String> args = new ArrayList<>();
        for (String setting : Cfg03Profiles.exercise3()) {
            args.add("--" + setting);
        }
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Cfg03Application.class)
                .web(WebApplicationType.NONE).run(args.toArray(new String[0]))) {
            assertThat(context.getEnvironment().getActiveProfiles()).contains("cfg03", "cfg03-dev", "cfg03-audit");
            assertThat(context.getBeansOfType(AuditLog.class)).hasSize(1);
        }
    }
}
