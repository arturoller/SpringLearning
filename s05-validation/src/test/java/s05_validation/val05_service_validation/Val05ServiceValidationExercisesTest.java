package s05_validation.val05_service_validation;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Val05ServiceValidation. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
class Val05ServiceValidationExercisesTest {

    @Autowired
    Exercise1Service exercise1Service;

    @Autowired
    Exercise2Service exercise2Service;

    @EnableConfigurationProperties(ExerciseProperties.class)
    static class PropertiesConfig {
    }

    @Test
    @DisplayName("Ćwiczenie 1: procent 0..100 i cena >= 0 pilnowane przez serwis")
    void exercise1() {
        assertThat(exercise1Service.discountedPrice(1000, 15)).isEqualTo(850);
        assertThatThrownBy(() -> exercise1Service.discountedPrice(1000, 150)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> exercise1Service.discountedPrice(-1, 10)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Ćwiczenie 2: parametr obowiązkowy i sprawdzany w głąb, wynik zgodny ze wzorcem")
    void exercise2() {
        assertThat(exercise2Service.codeFor(new ProductCommand("mleko", 349))).isEqualTo("MLE-0349");
        assertThatThrownBy(() -> exercise2Service.codeFor(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> exercise2Service.codeFor(new ProductCommand(" ", 100))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> exercise2Service.codeFor(new ProductCommand("ab", 100)))
                .isInstanceOfSatisfying(ConstraintViolationException.class, e ->
                        assertThat(e.getConstraintViolations()).extracting(v -> v.getPropertyPath().toString())
                                .containsExactly("codeFor.<return value>"));
    }

    @Test
    @DisplayName("Ćwiczenie 3: złe ustawienia zatrzymują start (BindValidationException)")
    void exercise3() {
        var runner = new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);
        String[] ok = {"val05.exercise.host=mail.sklep.pl", "val05.exercise.port=587", "val05.exercise.admin.email=admin@sklep.pl"};

        runner.withPropertyValues(ok).run(context -> assertThat(context).hasNotFailed());

        runner.withPropertyValues("val05.exercise.host=", "val05.exercise.port=587", "val05.exercise.admin.email=admin@sklep.pl")
                .run(context -> assertThat(rootOf(context.getStartupFailure())).isInstanceOf(BindValidationException.class));
        runner.withPropertyValues("val05.exercise.host=h", "val05.exercise.port=0", "val05.exercise.admin.email=admin@sklep.pl")
                .run(context -> assertThat(rootOf(context.getStartupFailure())).isInstanceOf(BindValidationException.class));
        runner.withPropertyValues("val05.exercise.host=h", "val05.exercise.port=70000", "val05.exercise.admin.email=admin@sklep.pl")
                .run(context -> assertThat(rootOf(context.getStartupFailure())).isInstanceOf(BindValidationException.class));
        runner.withPropertyValues("val05.exercise.host=h", "val05.exercise.port=25")   // brak admina
                .run(context -> assertThat(rootOf(context.getStartupFailure())).isInstanceOf(BindValidationException.class));
        runner.withPropertyValues("val05.exercise.host=h", "val05.exercise.port=25", "val05.exercise.admin.email=nie-email")
                .run(context -> assertThat(rootOf(context.getStartupFailure())).isInstanceOf(BindValidationException.class));
    }

    private static Throwable rootOf(Throwable failure) {
        Throwable root = failure;
        while (root != null && root.getCause() != null) {
            root = root.getCause();
        }
        return root;
    }
}
