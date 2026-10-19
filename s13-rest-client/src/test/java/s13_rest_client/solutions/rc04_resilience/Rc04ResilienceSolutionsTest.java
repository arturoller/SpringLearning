package s13_rest_client.solutions.rc04_resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import s13_rest_client.solutions.rc04_resilience.Rc04ResilienceSolutions.SolutionQuoteService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Rc04ResilienceExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Rc04ResilienceSolutionsTest {

    @Configuration
    @EnableResilientMethods
    static class ResilienceOn {
    }

    private static Supplier<String> flaky(int failures, String result, AtomicInteger attempts) {
        return () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt <= failures) {
                throw new IllegalStateException("awaria " + attempt);
            }
            return result;
        };
    }

    @Test
    @DisplayName("Wzorzec 1: polityka — 2 ponowienia tylko dla HttpServerErrorException")
    void solution1() {
        RetryTemplate template = new RetryTemplate(Rc04ResilienceSolutions.solution1());
        AtomicInteger serverErrorAttempts = new AtomicInteger();
        AtomicInteger notFoundAttempts = new AtomicInteger();

        assertThatThrownBy(() -> template.invoke(() -> {
            serverErrorAttempts.incrementAndGet();
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        })).isInstanceOf(HttpServerErrorException.class);
        assertThatThrownBy(() -> template.invoke(() -> {
            notFoundAttempts.incrementAndGet();
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
        })).isInstanceOf(HttpClientErrorException.class);

        assertThat(serverErrorAttempts.get()).isEqualTo(3);
        assertThat(notFoundAttempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Wzorzec 2: czy wolno automatycznie ponowić zapytanie danej metody HTTP")
    void solution2() {
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.GET, false)).isTrue();
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.HEAD, false)).isTrue();
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.OPTIONS, false)).isTrue();
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.PUT, false)).isTrue();
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.DELETE, false)).isTrue();
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.POST, false)).isFalse();
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.PATCH, false)).isFalse();
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.POST, true)).isTrue();
        assertThat(Rc04ResilienceSolutions.solution2(HttpMethod.PATCH, true)).isTrue();
    }

    @Test
    @DisplayName("Wzorzec 3: ponawianie przez RetryTemplate.invoke — wynik albo ostatni wyjątek")
    void solution3() {
        AtomicInteger attempts = new AtomicInteger();
        assertThat(Rc04ResilienceSolutions.solution3(flaky(2, "cena: 79.00", attempts), 3)).isEqualTo("cena: 79.00");
        assertThat(attempts.get()).isEqualTo(3);

        AtomicInteger exhausted = new AtomicInteger();
        assertThatThrownBy(() -> Rc04ResilienceSolutions.solution3(flaky(10, "nigdy", exhausted), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("awaria 2");
    }

    @Test
    @DisplayName("Wzorzec 4: @Retryable na beanie — 2 ponowienia przy IllegalStateException, inne wyjątki bez ponowień")
    void solution4() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger wrongSkuAttempts = new AtomicInteger();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(ResilienceOn.class);
            context.registerBean("flakyQuotes", SolutionQuoteService.class,
                    () -> new SolutionQuoteService(flaky(2, "cena: 79.00", attempts)));
            context.registerBean("wrongSkuQuotes", SolutionQuoteService.class,
                    () -> new SolutionQuoteService(() -> {
                        wrongSkuAttempts.incrementAndGet();
                        throw new IllegalArgumentException("zły kod produktu");
                    }));
            context.refresh();

            assertThat(context.getBean("flakyQuotes", SolutionQuoteService.class).quote()).isEqualTo("cena: 79.00");
            assertThat(attempts.get()).isEqualTo(3);
            assertThatThrownBy(() -> context.getBean("wrongSkuQuotes", SolutionQuoteService.class).quote())
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(wrongSkuAttempts.get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Wzorzec 5: RetryTemplate.execute — komunikaty wszystkich awarii po wyczerpaniu prób")
    void solution5() {
        assertThat(Rc04ResilienceSolutions.solution5(flaky(3, "nigdy", new AtomicInteger()), 2))
                .containsExactly("awaria 1", "awaria 2", "awaria 3");
        assertThat(Rc04ResilienceSolutions.solution5(flaky(1, "cena: 79.00", new AtomicInteger()), 2)).isEmpty();
    }
}
