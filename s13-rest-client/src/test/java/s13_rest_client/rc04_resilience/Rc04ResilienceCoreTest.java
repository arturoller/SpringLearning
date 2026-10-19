package s13_rest_client.rc04_resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.resilience.InvocationRejectedException;
import org.springframework.resilience.annotation.EnableResilientMethods;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Testy lekcji Rc04Resilience — sekcje 1, 3, 4 i 7 na samym rdzeniu Springa (bez Boota i bez HTTP):
 * małe konteksty {@code AnnotationConfigApplicationContext} z {@code @EnableResilientMethods} albo bez,
 * oraz programowe {@code RetryTemplate}.
 */
class Rc04ResilienceCoreTest {

    @Configuration
    @EnableResilientMethods
    static class ResilienceOn {
    }

    @Configuration
    static class ResilienceOff {
    }

    private static FlakyOperation flakyBean(Class<?> config) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(config);
        context.registerBean(FlakyOperation.class, () -> new FlakyOperation(2));   // 2 awarie, potem sukces
        context.refresh();
        return context.getBean(FlakyOperation.class);
    }

    @Test
    @DisplayName("3. Z @EnableResilientMethods: bean jest proxy, dwie awarie są ponawiane, wynik w 3. próbie")
    void withEnableResilientMethods() {
        FlakyOperation operation = flakyBean(ResilienceOn.class);

        assertThat(AopUtils.isAopProxy(operation)).isTrue();
        assertThat(operation.call()).isEqualTo("OK w próbie 3");
        assertThat(operation.attempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("3. PUŁAPKA: pole czytane WPROST z proxy jest null — stan ma obiekt docelowy, nie proxy")
    void proxyFieldIsEmpty() {
        FlakyOperation operation = flakyBean(ResilienceOn.class);
        operation.call();

        assertThat(operation.attempts).isNull();       // pole obiektu proxy (klasa ...$$SpringCGLIB$$0) — puste
        assertThat(operation.attempts()).isEqualTo(3);  // metoda idzie przez proxy do prawdziwego obiektu
    }

    @Test
    @DisplayName("3. PUŁAPKA: bez @EnableResilientMethods @Retryable jest po cichu ignorowany (pytanie 2)")
    void withoutEnableResilientMethods() {
        FlakyOperation operation = flakyBean(ResilienceOff.class);

        assertThat(AopUtils.isAopProxy(operation)).isFalse();
        assertThatThrownBy(operation::call)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("awaria przejściowa nr 1");
        assertThat(operation.attempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. PUŁAPKA: obiekt z new to nie bean — żadnego proxy, żadnych ponowień")
    void plainNewObject() {
        FlakyOperation operation = new FlakyOperation(2);

        assertThatThrownBy(operation::call).isInstanceOf(IllegalStateException.class);
        assertThat(operation.attempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("4. @ConcurrencyLimit(REJECT): gdy limit zajęty, kolejne wywołanie dostaje od razu InvocationRejectedException")
    void concurrencyLimitReject() throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(ResilienceOn.class);
        context.registerBean(ExportJob.class);
        context.refresh();
        ExportJob job = context.getBean(ExportJob.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);

        Thread running = Thread.ofPlatform().start(() -> {
            try {
                job.export(started, finish);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        try {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();   // pierwszy eksport trwa
            assertThatThrownBy(() -> job.export(new CountDownLatch(1), new CountDownLatch(0)))
                    .isInstanceOf(InvocationRejectedException.class)
                    .hasMessage("Concurrency limit reached: 1 - not allowed to enter "
                            + "s13_rest_client.rc04_resilience.ExportJob.export");
        } finally {
            finish.countDown();
            running.join();
            context.close();
        }
    }

    @Test
    @DisplayName("7. RetryTemplate: polityka (ile prób, jakie wyjątki, opóźnienie) w kodzie, bez adnotacji i proxy")
    void retryTemplateSuccess() throws RetryException {
        RetryPolicy policy = RetryPolicy.builder()
                .maxRetries(3)
                .delay(Duration.ZERO)
                .includes(IllegalStateException.class)
                .build();
        RetryTemplate template = new RetryTemplate(policy);
        AtomicInteger attempts = new AtomicInteger();

        String result = template.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("awaria " + attempts.get());
            }
            return "cena: 79.00";
        });

        assertThat(result).isEqualTo("cena: 79.00");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("7. Po wyczerpaniu prób execute(...) rzuca RetryException: liczba ponowień, wszystkie wyjątki, ostatni jako przyczyna")
    void retryTemplateExhausted() {
        RetryTemplate template = new RetryTemplate(RetryPolicy.builder().maxRetries(2).delay(Duration.ZERO).build());
        AtomicInteger attempts = new AtomicInteger();
        Retryable<String> priceCall = new Retryable<>() {
            @Override
            public String execute() {
                throw new IllegalStateException("awaria " + attempts.incrementAndGet());
            }

            @Override
            public String getName() {
                return "cena KSI-001";   // nazwa operacji w komunikatach
            }
        };

        RetryException ex = catchThrowableOfType(RetryException.class, () -> template.execute(priceCall));

        assertThat(ex).hasMessage("Retry policy for operation 'cena KSI-001' exhausted; aborting execution");
        assertThat(ex.getRetryCount()).isEqualTo(2);
        assertThat(ex.getExceptions()).extracting(Throwable::getMessage).containsExactly("awaria 1", "awaria 2", "awaria 3");
        assertThat(ex.getCause()).hasMessage("awaria 3");
    }

    @Test
    @DisplayName("7. invoke(...) zamiast execute(...): po wyczerpaniu prób leci OSTATNI wyjątek, bez RetryException")
    void retryTemplateInvoke() {
        RetryTemplate template = new RetryTemplate(RetryPolicy.builder().maxRetries(1).delay(Duration.ZERO).build());
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> template.invoke(() -> {
            throw new IllegalStateException("awaria " + attempts.incrementAndGet());
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("awaria 2");
    }

    @Test
    @DisplayName("1. Wyjątek spoza includes nie jest ponawiany — jedna próba")
    void notIncludedExceptionIsNotRetried() {
        RetryTemplate template = new RetryTemplate(RetryPolicy.builder()
                .maxRetries(3).delay(Duration.ZERO).includes(IllegalStateException.class).build());
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> template.invoke(() -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("zły kod produktu");   // błąd „na stałe” — powtórka nic nie da
        })).isInstanceOf(IllegalArgumentException.class);
        assertThat(attempts.get()).isEqualTo(1);
    }
}
