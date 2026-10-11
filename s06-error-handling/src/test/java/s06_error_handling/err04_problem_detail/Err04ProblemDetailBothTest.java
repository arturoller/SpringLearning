package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcja 5: własny advice + {@code problemdetails.enabled=true} naraz — własny wypiera gotowy advice Boota. */
@WebMvcTest(value = ProductController.class,
        properties = {"err04.custom-advice=true", "spring.mvc.problemdetails.enabled=true"})
class Err04ProblemDetailBothTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("5. Przy obu ustawieniach naraz w kontekście jest tylko własny ResponseEntityExceptionHandler")
    void customAdviceReplacesBootAdvice() {
        assertThat(context.getBeansOfType(ResponseEntityExceptionHandler.class).values())
                .singleElement().isInstanceOf(ProblemDetailAdvice.class);
    }
}
