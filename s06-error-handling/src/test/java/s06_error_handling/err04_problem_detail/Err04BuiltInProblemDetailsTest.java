package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import s06_error_handling.TestHttp;
import s06_error_handling.TestHttp.Reply;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wbudowana obsługa Boota: {@code spring.mvc.problemdetails.enabled=true}, BEZ własnej porady
 * ({@code err04.own-advice=false}). Boot rejestruje wtedy gotową poradę {@code ProblemDetailsExceptionHandler}
 * (rozszerza ResponseEntityExceptionHandler). RANDOM_PORT, bo część odpowiedzi przechodzi przez /error.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"err04.own-advice=false", "spring.mvc.problemdetails.enabled=true"})
class Err04BuiltInProblemDetailsTest {

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Boot rejestruje dokładnie jedną poradę ResponseEntityExceptionHandler: ProblemDetailsExceptionHandler")
    void bootRegistersItsOwnAdvice() {
        assertThat(context.getBeansOfType(ResponseEntityExceptionHandler.class).values())
                .extracting(bean -> bean.getClass().getSimpleName())
                .containsExactly("ProblemDetailsExceptionHandler");
    }

    @Test
    @DisplayName("2. Wyjątki Spring MVC (405, 404, 400 z walidacji) dostają ciało problem+json")
    void springExceptionsBecomeProblemJson() {
        Reply notAllowed = TestHttp.send("DELETE", port, "/api/products/ELE-001", "application/json", null, null);
        assertThat(notAllowed.status()).isEqualTo(405);
        assertThat(notAllowed.contentType()).startsWith("application/problem+json");
        assertThat(notAllowed.json()).containsEntry("status", 405).containsEntry("instance", "/api/products/ELE-001");

        Reply notFound = TestHttp.get(port, "/api/nie-ma-takiego");
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.contentType()).startsWith("application/problem+json");

        Reply invalid = TestHttp.postJson(port, "/api/products", "{\"name\":\" \",\"price\":-2}");
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.contentType()).startsWith("application/problem+json");
        assertThat(invalid.json()).doesNotContainKey("errors");   // wbudowana porada nie zna naszego pola errors
    }

    @Test
    @DisplayName("3. ErrorResponseException z własnym ProblemDetail działa od razu (409 + pola sku, available)")
    void errorResponseExceptionWorks() {
        Reply reply = TestHttp.postJson(port, "/api/products/ELE-001/reserve?quantity=10", "");

        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.contentType()).startsWith("application/problem+json");
        assertThat(reply.json()).containsEntry("sku", "ELE-001").containsEntry("available", 7);
    }

    @Test
    @DisplayName("4. PUŁAPKA: nasz zwykły wyjątek domeny (ProductNotFoundException) nadal kończy jako 500 z domyślnym JSON-em Boota")
    void domainExceptionStillGives500() {
        Reply reply = TestHttp.get(port, "/api/products/XXX");

        assertThat(reply.status()).isEqualTo(500);
        assertThat(reply.contentType()).startsWith("application/json");
        assertThat(reply.json()).containsOnlyKeys("timestamp", "status", "error", "path");
    }
}
