package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import s06_error_handling.TestHttp;

import static org.assertj.core.api.Assertions.assertThat;

/** Wartość {@code on-param} (po parametrze): komunikat pojawia się tylko, gdy zapytanie ma parametr {@code ?message=true}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.web.error.include-message=on-param")
class Err01DefaultErrorsOnParamTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("1. on-param: bez parametru i z ?message=false komunikatu nie ma, z ?message=true jest")
    void messageOnlyOnParam() {
        assertThat(TestHttp.get(port, "/api/products/KSI-999").json()).doesNotContainKey("message");
        assertThat(TestHttp.get(port, "/api/products/KSI-999?message=false").json()).doesNotContainKey("message");
        assertThat(TestHttp.get(port, "/api/products/KSI-999?message=true").json()).containsKey("message");
    }
}
