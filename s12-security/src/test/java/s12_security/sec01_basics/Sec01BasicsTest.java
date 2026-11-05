package s12_security.sec01_basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Sekcje 3, 4 i 5 lekcji Sec01Basics. {@code @WebMvcTest} (wycinek webowy) wystarcza: filtry Spring Security działają także
 * w MockMvc, a my sprawdzamy tylko statusy, nagłówki i JSON.
 * <p>
 * {@code @Import(Sec01SecurityConfig.class)} jest KONIECZNY: wycinek nie ładuje skanowanych klas {@code @Configuration},
 * więc bez niego działałby domyślny łańcuch Boota (test Sec01SliceWithoutImportTest).
 * {@code httpBasic("anna", "haslo-testowe")} (z spring-security-test) dopisuje do żądania prawdziwy nagłówek
 * {@code Authorization: Basic …}, więc logowanie przechodzi przez {@code BasicAuthenticationFilter} i nasz UserDetailsService.
 */
@WebMvcTest(Sec01Controller.class)
@Import(Sec01SecurityConfig.class)
class Sec01BasicsTest {

    @Autowired
    MockMvcTester mvc;   // MockMvcTester = MockMvc z asercjami AssertJ (s11_testing/Test03MockMvcTester)

    @Test
    @DisplayName("Sekcja 3: adres z permitAll() działa bez logowania, a każdy inny wymaga zalogowania (401)")
    void publicAndAuthenticated() {
        assertThat(mvc.get().uri("/sec01/public/hello"))
                .hasStatusOk()
                .hasBodyTextEqualTo("Witaj, gościu! Ten adres jest publiczny.");
        assertThat(mvc.get().uri("/sec01/hello")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/sec01/hello").with(httpBasic("anna", "haslo-testowe")))
                .hasStatusOk()
                .hasBodyTextEqualTo("Cześć, anna!");
    }

    @Test
    @DisplayName("Sekcja 3: przeglądarka (Accept: text/html) bez logowania jest przekierowana na formularz /login")
    void browserGoesToLoginForm() {
        assertThat(mvc.get().uri("/sec01/hello").accept(MediaType.TEXT_HTML))
                .hasStatus(HttpStatus.FOUND)                                   // 302 = przekierowanie
                .hasHeader(HttpHeaders.LOCATION, "/login");
        assertThat(mvc.get().uri("/login").accept(MediaType.TEXT_HTML))       // formularz generuje sam Spring Security
                .hasStatusOk()
                .bodyText().contains("<form", "username", "password");
    }

    @Test
    @DisplayName("Sekcja 4: 401 = nie wiemy, kim jesteś (z nagłówkiem WWW-Authenticate); 403 = wiemy, ale nie wolno")
    void unauthorizedVersusForbidden() {
        assertThat(mvc.get().uri("/sec01/admin/panel"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .headers().hasValue(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm\", charset=\"UTF-8\"");
        assertThat(mvc.get().uri("/sec01/admin/panel").with(httpBasic("anna", "zle-haslo")))
                .hasStatus(HttpStatus.UNAUTHORIZED);                           // złe hasło to też 401
        assertThat(mvc.get().uri("/sec01/admin/panel").with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN)
                .doesNotContainHeader(HttpHeaders.WWW_AUTHENTICATE);           // logowanie nic nie zmieni — nie ma o co prosić
        assertThat(mvc.get().uri("/sec01/admin/panel").with(httpBasic("admin", "haslo-testowe")))
                .hasStatusOk();
    }

    @Test
    @DisplayName("Sekcja 5: Authentication, Principal i @AuthenticationPrincipal opisują tego samego użytkownika; Security 7 dodaje FACTOR_PASSWORD")
    void whoAmI() {
        assertThat(mvc.get().uri("/sec01/me").with(httpBasic("anna", "haslo-testowe")))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"name": "anna", "principalName": "anna", "userDetailsName": "anna",
                         "authorities": ["FACTOR_PASSWORD", "ROLE_CUSTOMER"], "sameAsSecurityContext": true}
                        """);
    }

    @Test
    @WithMockUser(username = "zofia", roles = {"EMPLOYEE", "ADMIN"})
    @DisplayName("Sekcja 5: @WithMockUser wstawia gotowego użytkownika do kontekstu — bez hasła i bez UserDetailsService")
    void withMockUser() {
        assertThat(mvc.get().uri("/sec01/me"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"name": "zofia", "authorities": ["ROLE_ADMIN", "ROLE_EMPLOYEE"]}
                        """);
    }
}
