package s12_security.sec01_basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ĆWICZENIA 5 lekcji Sec01Basics. @Tag("cwiczenie") — uruchamiasz sam. Kontekst budujemy ręcznie
 * ({@code @ContextConfiguration}): kontroler ćwiczeń + {@link ExerciseSecurityBeans}, które wołają Twoje
 * {@link ExerciseSecurityConfig#filterChain}. Reguły lekcji (Sec01SecurityConfig) NIE są tu załadowane.
 */
@Tag("cwiczenie")
@WebMvcTest
@ContextConfiguration(classes = {ExerciseController.class, ExerciseSecurityBeans.class})
class Sec01ExerciseSecurityTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Ćwiczenie 5: /sec01/cw/public/** jest publiczne")
    void publicPath() {
        assertThat(mvc.get().uri("/sec01/cw/public/info")).hasStatusOk().hasBodyTextEqualTo("Sklep czynny 8–20");
    }

    @Test
    @DisplayName("Ćwiczenie 5: /sec01/cw/admin/** — anonim 401")
    void adminAnonymous() {
        assertThat(mvc.get().uri("/sec01/cw/admin/stats")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Ćwiczenie 5: /sec01/cw/admin/** — klient 403")
    void adminCustomer() {
        assertThat(mvc.get().uri("/sec01/cw/admin/stats")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Ćwiczenie 5: /sec01/cw/admin/** — administrator 200")
    void adminAdmin() {
        assertThat(mvc.get().uri("/sec01/cw/admin/stats")).hasStatusOk();
    }
}
