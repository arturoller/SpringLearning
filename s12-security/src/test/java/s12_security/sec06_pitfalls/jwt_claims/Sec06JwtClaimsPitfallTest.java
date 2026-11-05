package s12_security.sec06_pitfalls.jwt_claims;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import s12_security.sec06_security_testing.Sec06Application;
import s12_security.sec06_security_testing.Sec06SecurityConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Sekcja 2 lekcji Sec06SecurityTesting — PUŁAPKA: {@code with(jwt().jwt(j -> j.claim("roles", …)))} NIE używa konwertera
 * aplikacji. Post-procesor sam liczy uprawnienia domyślnym sposobem (z {@code scope}), więc twierdzenie {@code roles} nic nie
 * daje. Aplikacja z prawdziwym tokenem działa dobrze — "psuje się" tylko test.
 */
@SpringBootTest(classes = Sec06Application.class)
@AutoConfigureMockMvc
class Sec06JwtClaimsPitfallTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 2 (pułapka): jwt() z twierdzeniem roles: [ADMIN] → 403; ten sam jwt() z konwerterem aplikacji → 200")
    void jwtPostProcessorIgnoresAppConverter() {
        assertThat(mvc.get().uri("/sec06/admin/stats").with(jwt().jwt(token -> token.claim("roles", List.of("ADMIN")))))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec06/admin/stats").with(jwt().jwt(token -> token.claim("roles", List.of("ADMIN")))
                .authorities(Sec06SecurityConfig.rolesAuthorities())))
                .hasStatusOk();
    }
}
