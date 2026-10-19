package s13_rest_client.rc04_resilience;

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
import s13_rest_client.rc04_resilience.Rc04Resilience.ExerciseQuoteService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Rc04Resilience. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Bez Boota i bez HTTP. */
@Tag("cwiczenie")
class Rc04ResilienceExercisesTest {

    @Configuration
    @EnableResilientMethods
    static class ResilienceOn {
    }

    /** Wywołanie, które najpierw {@code failures} razy rzuca IllegalStateException("awaria N"), potem zwraca result. */
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
    @DisplayName("Ćwiczenie 1: polityka — 2 ponowienia tylko dla HttpServerErrorException")
    void exercise1() {
        RetryTemplate template = new RetryTemplate(Rc04Resilience.exercise1());
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

        assertThat(serverErrorAttempts.get()).as("503: 1 próba + 2 ponowienia").isEqualTo(3);
        assertThat(notFoundAttempts.get()).as("404: bez ponowień").isEqualTo(1);
    }

    @Test
    @DisplayName("Ćwiczenie 2: czy wolno automatycznie ponowić zapytanie danej metody HTTP")
    void exercise2() {
        assertThat(Rc04Resilience.exercise2(HttpMethod.GET, false)).isTrue();
        assertThat(Rc04Resilience.exercise2(HttpMethod.HEAD, false)).isTrue();
        assertThat(Rc04Resilience.exercise2(HttpMethod.OPTIONS, false)).isTrue();
        assertThat(Rc04Resilience.exercise2(HttpMethod.PUT, false)).isTrue();
        assertThat(Rc04Resilience.exercise2(HttpMethod.DELETE, false)).isTrue();
        assertThat(Rc04Resilience.exercise2(HttpMethod.POST, false)).isFalse();
        assertThat(Rc04Resilience.exercise2(HttpMethod.PATCH, false)).isFalse();
        assertThat(Rc04Resilience.exercise2(HttpMethod.POST, true)).isTrue();
        assertThat(Rc04Resilience.exercise2(HttpMethod.PATCH, true)).isTrue();
    }

    @Test
    @DisplayName("Ćwiczenie 3: ponawianie przez RetryTemplate.invoke — wynik albo ostatni wyjątek")
    void exercise3() {
        AtomicInteger attempts = new AtomicInteger();
        assertThat(Rc04Resilience.exercise3(flaky(2, "cena: 79.00", attempts), 3)).isEqualTo("cena: 79.00");
        assertThat(attempts.get()).isEqualTo(3);

        AtomicInteger exhausted = new AtomicInteger();
        assertThatThrownBy(() -> Rc04Resilience.exercise3(flaky(10, "nigdy", exhausted), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("awaria 2");
    }

    @Test
    @DisplayName("Ćwiczenie 4: @Retryable na beanie — 2 ponowienia przy IllegalStateException, inne wyjątki bez ponowień")
    void exercise4() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger wrongSkuAttempts = new AtomicInteger();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(ResilienceOn.class);
            context.registerBean("flakyQuotes", ExerciseQuoteService.class,
                    () -> new ExerciseQuoteService(flaky(2, "cena: 79.00", attempts)));
            context.registerBean("wrongSkuQuotes", ExerciseQuoteService.class,
                    () -> new ExerciseQuoteService(() -> {
                        wrongSkuAttempts.incrementAndGet();
                        throw new IllegalArgumentException("zły kod produktu");
                    }));
            context.refresh();

            assertThat(context.getBean("flakyQuotes", ExerciseQuoteService.class).quote()).isEqualTo("cena: 79.00");
            assertThat(attempts.get()).isEqualTo(3);
            assertThatThrownBy(() -> context.getBean("wrongSkuQuotes", ExerciseQuoteService.class).quote())
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(wrongSkuAttempts.get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Ćwiczenie 5: RetryTemplate.execute — komunikaty wszystkich awarii po wyczerpaniu prób")
    void exercise5() {
        assertThat(Rc04Resilience.exercise5(flaky(3, "nigdy", new AtomicInteger()), 2))
                .containsExactly("awaria 1", "awaria 2", "awaria 3");
        assertThat(Rc04Resilience.exercise5(flaky(1, "cena: 79.00", new AtomicInteger()), 2)).isEmpty();
    }
}
