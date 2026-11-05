package s12_security.sec02_pitfalls.any_request_first;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sekcja 1 lekcji Sec02Authorization — {@code anyRequest()} musi być ostatnie; Spring Security pilnuje tego przy starcie. */
class Sec02AnyRequestFirstPitfallTest {

    @Test
    @DisplayName("Sekcja 1 (pułapka): requestMatchers po anyRequest() — aplikacja nie startuje (IllegalStateException)")
    void applicationDoesNotStart() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(AnyRequestFirstApplication.class).properties("server.port=0").run())
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can't configure requestMatchers after anyRequest");
    }
}
