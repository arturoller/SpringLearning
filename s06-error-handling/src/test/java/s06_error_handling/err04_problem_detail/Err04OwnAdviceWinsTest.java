package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PUŁAPKA: własna porada rozszerzająca ResponseEntityExceptionHandler WYŁĄCZA wbudowaną poradę Boota
 * ({@code @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)}) — nawet gdy właściwość jest włączona.
 */
@SpringBootTest(properties = "spring.mvc.problemdetails.enabled=true")
class Err04OwnAdviceWinsTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Własna ProblemAdvice + problemdetails.enabled=true → w kontekście jest tylko ProblemAdvice")
    void onlyOwnAdviceIsRegistered() {
        assertThat(context.getBeansOfType(ResponseEntityExceptionHandler.class).values())
                .extracting(bean -> bean.getClass().getSimpleName())
                .containsExactly("ProblemAdvice");
    }
}
