package s12_security.sec02_pitfalls.catch_all;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcja 3 lekcji Sec02Authorization — pułapka: advice "łap wszystko" połyka odmowę z {@code @PreAuthorize}. */
@SpringBootTest
@AutoConfigureMockMvc
class Sec02CatchAllPitfallTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Sekcja 3 (pułapka): handler Exception.class zamienia AuthorizationDeniedException w 500 zamiast 403")
    void deniedBecomes500() {
        assertThat(mvc.get().uri("/sec02/pitfall/stats")).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @WithMockUser(username = "boss", roles = "ADMIN")
    @DisplayName("Sekcja 3: dla admina wszystko działa — dlatego tę pułapkę widać dopiero przy teście z inną rolą")
    void adminIsFine() {
        assertThat(mvc.get().uri("/sec02/pitfall/stats")).hasStatusOk().hasBodyTextEqualTo("Statystyki");
    }
}
