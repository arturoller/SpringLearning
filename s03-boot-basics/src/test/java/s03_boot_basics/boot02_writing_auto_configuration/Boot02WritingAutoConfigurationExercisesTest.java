package s03_boot_basics.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot02WritingAutoConfiguration. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
@ActiveProfiles("boot02")
class Boot02WritingAutoConfigurationExercisesTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Ćwiczenie 1: powitanie z beana GreetingService")
    void exercise1() {
        assertThat(Boot02WritingAutoConfiguration.exercise1(context))
                .isEqualTo("Witaj, Ania! Zapraszamy do sklepu Sklep pod Sprezyna.");
    }

    @Test
    @DisplayName("Ćwiczenie 2: własny GreetingService wygrywa z domyślnym")
    void exercise2() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Boot02GreetingAutoConfiguration.class))
                .withPropertyValues("boot02.greeting.enabled=true")
                .withUserConfiguration(Boot02ExerciseGreetingConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(GreetingService.class);
                    assertThat(ctx.getBean(GreetingService.class).greet("Ola")).isEqualTo("Dzień dobry, Ola!");
                });
    }

    @Test
    @DisplayName("Ćwiczenie 3: autokonfiguracja programu lojalnościowego")
    void exercise3() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(Boot02ExerciseLoyaltyAutoConfiguration.class));

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
