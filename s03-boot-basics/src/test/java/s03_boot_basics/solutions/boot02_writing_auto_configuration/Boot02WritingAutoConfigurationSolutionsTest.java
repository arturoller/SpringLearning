package s03_boot_basics.solutions.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import s03_boot_basics.boot02_writing_auto_configuration.Boot02Application;
import s03_boot_basics.boot02_writing_auto_configuration.Boot02GreetingAutoConfiguration;
import s03_boot_basics.boot02_writing_auto_configuration.GreetingService;
import s03_boot_basics.boot02_writing_auto_configuration.LoyaltyPolicy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Boot02WritingAutoConfigurationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma własnej klasy startowej, więc wskazujemy ją wprost: {@code classes = Boot02Application.class}.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Boot02Application.class)
@ActiveProfiles("boot02")
class Boot02WritingAutoConfigurationSolutionsTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Wzorzec 1: powitanie z beana GreetingService")
    void solution1() {
        assertThat(Boot02WritingAutoConfigurationSolutions.solution1(context))
                .isEqualTo("Witaj, Ania! Zapraszamy do sklepu Sklep pod Sprezyna.");
    }

    @Test
    @DisplayName("Wzorzec 2: własny GreetingService wygrywa z domyślnym")
    void solution2() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Boot02GreetingAutoConfiguration.class))
                .withPropertyValues("boot02.greeting.enabled=true")
                .withUserConfiguration(Boot02ExerciseGreetingConfigSolution.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(GreetingService.class);
                    assertThat(ctx.getBean(GreetingService.class).greet("Ola")).isEqualTo("Dzień dobry, Ola!");
                });
    }

    @Test
    @DisplayName("Wzorzec 3: autokonfiguracja programu lojalnościowego")
    void solution3() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Boot02LoyaltyAutoConfigurationSolution.class));

        runner.run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(LoyaltyPolicy.class));
        runner.withPropertyValues("boot02.loyalty.enabled=true")
                .run(ctx -> assertThat(ctx.getBean(LoyaltyPolicy.class).percent()).isEqualTo(5));
        runner.withPropertyValues("boot02.loyalty.enabled=true", "boot02.loyalty.percent=10")
                .run(ctx -> assertThat(ctx.getBean(LoyaltyPolicy.class).discounted(1000)).isEqualTo(900));
        runner.withPropertyValues("boot02.loyalty.enabled=true")
                .withBean(LoyaltyPolicy.class, () -> new LoyaltyPolicy(20))
                .run(ctx -> assertThat(ctx).hasSingleBean(LoyaltyPolicy.class)
                        .getBean(LoyaltyPolicy.class).extracting(LoyaltyPolicy::percent).isEqualTo(20));
    }
}
