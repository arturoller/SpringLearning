package s03_boot_basics.solutions.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import s03_boot_basics.boot02_writing_auto_configuration.ShopFarewell;
import s03_boot_basics.boot02_writing_auto_configuration.ShopGreeter;
import s03_boot_basics.boot02_writing_auto_configuration.ShopGreeterAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Boot02WritingAutoConfigurationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną
 * zawsze. Metoda solution2 to wzorcowe rozwiązanie ćwiczenia 2 (ćwiczenie polega na napisaniu testu).
 */
@Tag("wzorzec")
class Boot02WritingAutoConfigurationSolutionsTest {

    @EnableConfigurationProperties(Boot02WritingAutoConfigurationSolutions.ExerciseGreeterProperties.class)
    static class EnableExerciseProperties {
    }

    /** Konfiguracja użytkownika do rozwiązania 2 (bez @Configuration — nie trafi do skanowania). */
    static class VipGreeterConfig {
        @Bean
        ShopGreeter vipGreeter() {
            return new ShopGreeter("SpringLearning VIP", "Szanowny Panie");
        }
    }

    static class UserFarewellConfig {
        @Bean
        ShopFarewell myFarewell() {
            return new ShopFarewell();
        }
    }

    @Test
    @DisplayName("Wzorzec 1: rekord ustawień ma wartości domyślne SpringLearning i 100")
    void solution1() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(EnableExerciseProperties.class);
        runner.run(ctx -> {
            Boot02WritingAutoConfigurationSolutions.ExerciseGreeterProperties properties =
                    ctx.getBean(Boot02WritingAutoConfigurationSolutions.ExerciseGreeterProperties.class);
            assertThat(properties.shopName()).isEqualTo("SpringLearning");
            assertThat(properties.maxGreetingsPerDay()).isEqualTo(100);
        });
        runner.withPropertyValues("s03.exercise.greeter.shop-name=Outlet", "s03.exercise.greeter.max-greetings-per-day=5")
                .run(ctx -> {
                    Boot02WritingAutoConfigurationSolutions.ExerciseGreeterProperties properties =
                            ctx.getBean(Boot02WritingAutoConfigurationSolutions.ExerciseGreeterProperties.class);
                    assertThat(properties.shopName()).isEqualTo("Outlet");
                    assertThat(properties.maxGreetingsPerDay()).isEqualTo(5);
                });
    }

    @Test
    @DisplayName("Wzorzec 2: bean użytkownika wygrywa z autokonfiguracją biblioteki")
    void solution2() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ShopGreeterAutoConfiguration.class))
                .withPropertyValues("s03.greeter.enabled=true", "s03.greeter.greeting=Siema")
                .withUserConfiguration(VipGreeterConfig.class)
                .run(ctx -> {
                    // (a) dokładnie jeden ShopGreeter — użytkownika
                    assertThat(ctx).hasSingleBean(ShopGreeter.class);
                    assertThat(ctx.getBean(ShopGreeter.class)).isSameAs(ctx.getBean("vipGreeter"));
                    // (b) metoda biblioteki została pominięta
                    assertThat(ctx).doesNotHaveBean("shopGreeter");
                    // (c) ustawienia biblioteki nie dotyczą beana użytkownika
                    assertThat(ctx.getBean(ShopGreeter.class).greet("Jan Kowalski"))
                            .doesNotStartWith("Siema")
                            .startsWith("Szanowny Panie");
                });
    }

    @Test
    @DisplayName("Wzorzec 3: ShopFarewell powstaje tylko obok ShopGreeter i ustępuje beanowi użytkownika")
    void solution3() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ShopGreeterAutoConfiguration.class,
                        Boot02WritingAutoConfigurationSolutions.FarewellAutoConfiguration.class));

        runner.withPropertyValues("s03.greeter.enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(ShopFarewell.class);
            assertThat(ctx.getBean(ShopFarewell.class).farewell("Jan Kowalski"))
                    .isEqualTo("Do widzenia, Jan Kowalski! Zapraszamy ponownie.");
        });
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ShopFarewell.class));
        runner.withPropertyValues("s03.greeter.enabled=true").withUserConfiguration(UserFarewellConfig.class)
                .run(ctx -> assertThat(ctx.getBeanNamesForType(ShopFarewell.class)).containsExactly("myFarewell"));
    }
}
