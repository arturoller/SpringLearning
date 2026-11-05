package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Err04ProblemDetail: gdy w aplikacji jest własny {@code ResponseEntityExceptionHandler}, Boot nie rejestruje
 * swojego (warunek {@code @ConditionalOnMissingBean}) — nawet przy włączonej właściwości. Dzięki temu nie ma dwóch konkurujących handlerów.
 */
@WebMvcTest(value = ShopController.class, properties = "spring.mvc.problemdetails.enabled=true")
class Err04OwnAdviceDisablesBuiltInTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 4: własny advice wyłącza wbudowany handler, nawet gdy spring.mvc.problemdetails.enabled=true")
    void ownAdviceWins() {
        assertThat(context.getBeansOfType(Err04Advice.class)).hasSize(1);
        assertThat(context.containsBean("problemDetailsExceptionHandler")).isFalse();
    }
}
