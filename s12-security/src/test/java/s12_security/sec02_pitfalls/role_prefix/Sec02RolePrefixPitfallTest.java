package s12_security.sec02_pitfalls.role_prefix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 2 lekcji Sec02Authorization — pułapka {@code hasRole("ROLE_ADMIN")}. Uruchamiamy aplikację ręcznie
 * ({@code SpringApplicationBuilder}), bo sprawdzamy, że w ogóle NIE wystartuje. {@code server.port=0} = losowy wolny port.
 */
class Sec02RolePrefixPitfallTest {

    @Test
    @DisplayName("Sekcja 2 (pułapka): hasRole(\"ROLE_ADMIN\") w regułach adresów — aplikacja nie startuje (IllegalArgumentException)")
    void applicationDoesNotStart() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(RolePrefixApplication.class).properties("server.port=0").run())
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ROLE_ADMIN should not start with ROLE_ since ROLE_ is automatically prepended");
    }
}
