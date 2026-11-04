package s12_security.sec06_pitfalls.no_filters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import s12_security.sec06_security_testing.Sec06Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Sec06SecurityTesting — PUŁAPKA: {@code @AutoConfigureMockMvc(addFilters = false)}. MockMvc pomija WSZYSTKIE
 * filtry serwletu, a więc cały Spring Security z reguł adresów, CSRF i logowaniem. Testy robią się zielone, bo nic nie jest
 * sprawdzane. Ta sama aplikacja co w lekcji ({@code classes = Sec06Application.class}), tylko MockMvc bez filtrów.
 */
@SpringBootTest(classes = Sec06Application.class)
@AutoConfigureMockMvc(addFilters = false)
class Sec06NoFiltersPitfallTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 5 (pułapka): bez filtrów anonim wchodzi do panelu admina (200) i zmienia cenę bez CSRF (200)")
    void everythingIsOpen() {
        assertThat(mvc.get().uri("/sec06/admin/stats")).hasStatusOk();
        assertThat(mvc.post().uri("/sec06/panel/price").param("product", "kawa").param("price", "1.00")).hasStatusOk();
    }

    @Test
    @DisplayName("Sekcja 5 (pułapka): reguły METOD dalej działają (to proxy, nie filtr) — ale bez filtrów zamiast 401 lecą wyjątki")
    void methodSecurityStillOnButNo401() {
        assertThat(mvc.delete().uri("/sec06/orders/1"))                       // @PreAuthorize("hasRole('ADMIN')")
                .hasFailed()
                .failure().hasRootCauseInstanceOf(AuthenticationCredentialsNotFoundException.class);
        // Nie ma nawet anonima (robi go filtr AnonymousAuthenticationFilter), więc "authentication.name" w SpEL nie działa:
        assertThat(mvc.get().uri("/sec06/orders/of/piotr"))                  // @PreAuthorize("#username == authentication.name …")
                .hasFailed()
                .failure().hasMessageContaining("Failed to evaluate expression");
    }
}
