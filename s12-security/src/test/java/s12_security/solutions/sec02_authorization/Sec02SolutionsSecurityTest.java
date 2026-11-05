package s12_security.solutions.sec02_authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import s12_security.sec02_authorization.ExerciseController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test rozwiązania wzorcowego ćwiczenia 4 lekcji Sec02Authorization. @Tag("wzorzec"). Kontroler jest ten sam, gotowy
 * kontroler z lekcji; reguły — z rozwiązania ({@link ExerciseSecurityConfig}).
 */
@Tag("wzorzec")
@WebMvcTest
@ContextConfiguration(classes = {ExerciseController.class, ExerciseSecurityConfig.class})
class Sec02SolutionsSecurityTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Wzorzec 4: katalog publiczny, ale tajny prototyp — anonim 401")
    void anonymous() {
        assertThat(mvc.get().uri("/sec02/cw/catalog/coffee")).hasStatusOk();
        assertThat(mvc.get().uri("/sec02/cw/catalog/hidden/prototype")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Wzorzec 4: klient — tajny prototyp 403, dodawanie 403, adres bez reguły 403")
    void customer() {
        assertThat(mvc.get().uri("/sec02/cw/catalog/hidden/prototype")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/sec02/cw/catalog")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec02/cw/secret")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "ewa", roles = "EMPLOYEE")
    @DisplayName("Wzorzec 4: pracownik — prototyp 200, dodawanie 201, ale adres bez reguły dalej 403 (deny by default)")
    void employee() {
        assertThat(mvc.get().uri("/sec02/cw/catalog/hidden/prototype")).hasStatusOk();
        assertThat(mvc.post().uri("/sec02/cw/catalog")).hasStatus(HttpStatus.CREATED);
        assertThat(mvc.get().uri("/sec02/cw/secret")).hasStatus(HttpStatus.FORBIDDEN);
    }
}
