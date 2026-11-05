package s12_security.sec02_authorization;

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
 * Test ĆWICZENIA 4 lekcji Sec02Authorization. @Tag("cwiczenie") — uruchamiasz sam. Kontekst ręczny: gotowy ExerciseController
 * + {@link ExerciseSecurityBeans}, które wołają Twoje {@link ExerciseSecurityConfig#filterChain}.
 */
@Tag("cwiczenie")
@WebMvcTest
@ContextConfiguration(classes = {ExerciseController.class, ExerciseSecurityBeans.class})
class Sec02ExerciseSecurityTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Ćwiczenie 4: katalog publiczny, ale tajny prototyp — anonim 401")
    void anonymous() {
        assertThat(mvc.get().uri("/sec02/cw/catalog/coffee")).hasStatusOk();
        assertThat(mvc.get().uri("/sec02/cw/catalog/hidden/prototype")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Ćwiczenie 4: klient — tajny prototyp 403, dodawanie 403, adres bez reguły 403")
    void customer() {
        assertThat(mvc.get().uri("/sec02/cw/catalog/hidden/prototype")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/sec02/cw/catalog")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec02/cw/secret")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "ewa", roles = "EMPLOYEE")
    @DisplayName("Ćwiczenie 4: pracownik — prototyp 200, dodawanie 201, ale adres bez reguły dalej 403 (deny by default)")
    void employee() {
        assertThat(mvc.get().uri("/sec02/cw/catalog/hidden/prototype")).hasStatusOk();
        assertThat(mvc.post().uri("/sec02/cw/catalog")).hasStatus(HttpStatus.CREATED);
        assertThat(mvc.get().uri("/sec02/cw/secret")).hasStatus(HttpStatus.FORBIDDEN);
    }
}
