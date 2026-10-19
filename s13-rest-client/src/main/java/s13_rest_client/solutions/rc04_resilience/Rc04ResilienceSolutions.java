package s13_rest_client.solutions.rc04_resilience;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rc04Resilience — zajrzyj dopiero po własnej próbie! */
public final class Rc04ResilienceSolutions {

    private static final Set<HttpMethod> IDEMPOTENT =
            Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.PUT, HttpMethod.DELETE);

    private Rc04ResilienceSolutions() {
    }

    static RetryPolicy solution1() {
        return RetryPolicy.builder()
                .maxRetries(2)
                .delay(Duration.ZERO)
                .includes(HttpServerErrorException.class)
                .build();
    }

    static boolean solution2(HttpMethod method, boolean hasIdempotencyKey) {
        return IDEMPOTENT.contains(method) || hasIdempotencyKey;
    }

    static <T> T solution3(Supplier<T> call, int maxRetries) {
        RetryTemplate template = new RetryTemplate(RetryPolicy.builder().maxRetries(maxRetries).delay(Duration.ZERO).build());
        return template.invoke(call);
    }

    /** Ćwiczenie 4: ExerciseQuoteService z adnotacją @Retryable. */
    public static class SolutionQuoteService {

        private final Supplier<String> supplierCall;

        public SolutionQuoteService(Supplier<String> supplierCall) {
            this.supplierCall = supplierCall;
        }

        @Retryable(includes = IllegalStateException.class, maxRetries = 2, delay = 0)
        public String quote() {
            return supplierCall.get();
        }
    }

    static List<String> solution5(Supplier<String> call, int maxRetries) {
        RetryTemplate template = new RetryTemplate(RetryPolicy.builder().maxRetries(maxRetries).delay(Duration.ZERO).build());
        try {
            template.execute(call::get);
            return List.of();
        } catch (RetryException ex) {
            return ex.getExceptions().stream().map(Throwable::getMessage).toList();
        }
    }
}
