package s03_boot_basics.boot02_writing_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot02WritingAutoConfiguration. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot02WritingAutoConfigurationExercisesTest {

    /** Rejestruje rekord ustawień z ćwiczenia 1 (bez @Configuration — nie trafi do skanowania). */
    @EnableConfigurationProperties(Boot02WritingAutoConfiguration.ExerciseGreeterProperties.class)
    static class EnableExerciseProperties {
    }

    /** Własny ShopFarewell użytkownika do ćwiczenia 3. */
    static class UserFarewellConfig {
        @Bean
        ShopFarewell myFarewell() {
            return new ShopFarewell();
        }
    }

    @Test
    @DisplayName("Ćwiczenie 1: rekord ustawień ma wartości domyślne SpringLearning i 100")
    void exercise1() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(EnableExerciseProperties.class);
        runner.run(ctx -> {
            Boot02WritingAutoConfiguration.ExerciseGreeterProperties properties =
                    ctx.getBean(Boot02WritingAutoConfiguration.ExerciseGreeterProperties.class);
            assertThat(properties.shopName()).isEqualTo("SpringLearning");
            assertThat(properties.maxGreetingsPerDay()).isEqualTo(100);
        });
        runner.withPropertyValues("s03.exercise.greeter.shop-name=Outlet", "s03.exercise.greeter.max-greetings-per-day=5")
                .run(ctx -> {
                    Boot02WritingAutoConfiguration.ExerciseGreeterProperties properties =
                            ctx.getBean(Boot02WritingAutoConfiguration.ExerciseGreeterProperties.class);
                    assertThat(properties.shopName()).isEqualTo("Outlet");
                    assertThat(properties.maxGreetingsPerDay()).isEqualTo(5);
                });
    }

    @Test
    @DisplayName("Ćwiczenie 2: Twój test — bean użytkownika wygrywa z autokonfiguracją biblioteki")
    void exercise2() {
        // TODO: twoje rozwiązanie — opis w Boot02WritingAutoConfiguration (ĆWICZENIE 2). Usuń poniższą linię.
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3: ShopFarewell powstaje tylko obok ShopGreeter i ustępuje beanowi użytkownika")
    void exercise3() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ShopGreeterAutoConfiguration.class,
                        Boot02WritingAutoConfiguration.ExerciseFarewellAutoConfiguration.class));

        runner.withPropertyValues("s03.greeter.enabled=true").run(ctx -> {
            assertThat(ctx).as("przy włączonym ShopGreeter powinien powstać ShopFarewell").hasSingleBean(ShopFarewell.class);
            assertThat(ctx.getBean(ShopFarewell.class).farewell("Jan Kowalski"))
                    .isEqualTo("Do widzenia, Jan Kowalski! Zapraszamy ponownie.");
        });
        runner.run(ctx -> assertThat(ctx).as("bez ShopGreeter nie ma ShopFarewell").doesNotHaveBean(ShopFarewell.class));
        runner.withPropertyValues("s03.greeter.enabled=true").withUserConfiguration(UserFarewellConfig.class)
                .run(ctx -> assertThat(ctx.getBeanNamesForType(ShopFarewell.class)).containsExactly("myFarewell"));
    }
}
