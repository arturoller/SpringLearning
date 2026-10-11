package s05_validation.solutions.val05_service_validation;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import s05_validation.val05_service_validation.ProductCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Val05ServiceValidationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@SpringBootTest
class Val05ServiceValidationSolutionsTest {

    @Autowired
    Solution1Service solution1Service;

    @Autowired
    Solution2Service solution2Service;

    @EnableConfigurationProperties(SolutionProperties.class)
    static class PropertiesConfig {
    }

    @Test
    @DisplayName("Wzorzec 1: procent 0..100 i cena >= 0 pilnowane przez serwis")
    void solution1() {
        assertThat(solution1Service.discountedPrice(1000, 15)).isEqualTo(850);
        assertThatThrownBy(() -> solution1Service.discountedPrice(1000, 150)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> solution1Service.discountedPrice(-1, 10)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: parametr obowiązkowy i sprawdzany w głąb, wynik zgodny ze wzorcem")
    void solution2() {
        assertThat(solution2Service.codeFor(new ProductCommand("mleko", 349))).isEqualTo("MLE-0349");
        assertThatThrownBy(() -> solution2Service.codeFor(null)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> solution2Service.codeFor(new ProductCommand(" ", 100))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> solution2Service.codeFor(new ProductCommand("ab", 100)))
                .isInstanceOfSatisfying(ConstraintViolationException.class, e ->
                        assertThat(e.getConstraintViolations()).extracting(v -> v.getPropertyPath().toString())
                                .containsExactly("codeFor.<return value>"));
    }

    @Test
    @DisplayName("Wzorzec 3: złe ustawienia zatrzymują start (BindValidationException)")
    void solution3() {
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
