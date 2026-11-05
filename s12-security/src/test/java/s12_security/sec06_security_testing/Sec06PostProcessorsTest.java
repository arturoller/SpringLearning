package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 2 lekcji Sec06SecurityTesting: post-procesory żądań ({@code SecurityMockMvcRequestPostProcessors}) — użytkownik
 * przypięty do JEDNEGO żądania, a nie do całego testu. Działają tak samo w {@link MockMvcTester} ({@code .with(…)})
 * i w klasycznym {@link MockMvc} ({@code perform(get(…).with(…))}).
 */
@WebMvcTest
@Import({Sec06SecurityConfig.class, OrderService.class})
class Sec06PostProcessorsTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    MockMvc classicMvc;

    @Test
    @DisplayName("Sekcja 2: with(user(\"anna\").roles(\"CUSTOMER\")) — w jednym teście różni użytkownicy, różne statusy")
    void userPostProcessor() {
        assertThat(mvc.get().uri("/sec06/orders/of/piotr").with(user("anna").roles("CUSTOMER")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec06/orders/of/piotr").with(user("piotr").roles("CUSTOMER")))
                .hasStatusOk();
        assertThat(mvc.get().uri("/sec06/orders/of/piotr").with(anonymous()))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Sekcja 2: with(httpBasic(…)) wysyła prawdziwy nagłówek Basic — hasło sprawdza UserDetailsService (złe → 401)")
    void httpBasicPostProcessor() {
        assertThat(mvc.post().uri("/sec06/token").with(httpBasic("ewa", "haslo-testowe")))
                .hasStatusOk()
                .bodyJson().extractingPath("$.token_type").isEqualTo("Bearer");
        assertThat(mvc.post().uri("/sec06/token").with(httpBasic("ewa", "zle-haslo")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Sekcja 2: with(csrf()) — panel z sesją: POST bez tokenu CSRF 403, z tokenem 200")
    void csrfPostProcessor() {
        assertThat(mvc.post().uri("/sec06/panel/price").param("product", "kawa").param("price", "41.90")
                .with(user("ewa").roles("EMPLOYEE")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/sec06/panel/price").param("product", "kawa").param("price", "41.90")
                .with(user("ewa").roles("EMPLOYEE")).with(csrf()))
                .hasStatusOk()
                .hasBodyTextEqualTo("Nowa cena kawa: 41.90 zł");
    }

    @Test
    @DisplayName("Sekcja 2: with(jwt()) — gotowe uwierzytelnienie tokenem bez podpisywania: authorities(…) i jwt(j -> j.subject(…))")
    void jwtPostProcessor() {
        assertThat(mvc.get().uri("/sec06/admin/stats").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .hasStatusOk();
        assertThat(mvc.get().uri("/sec06/orders").with(jwt().jwt(token -> token.subject("piotr"))))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].id").asArray().containsExactly(3);
        assertThat(mvc.get().uri("/sec06/admin/stats")
                .with(jwt().jwt(token -> token.claim("roles", List.of("ADMIN")))
                        .authorities(Sec06SecurityConfig.rolesAuthorities())))          // ten sam konwerter co aplikacja
                .hasStatusOk();
    }

    @Test
    @DisplayName("Sekcja 2: klasyczny MockMvc (perform + andExpect) z tym samym post-procesorem — tak wygląda większość poradników")
    void classicMockMvc() throws Exception {
        classicMvc.perform(get("/sec06/admin/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        classicMvc.perform(get("/sec06/admin/stats").with(user("anna").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }
}
