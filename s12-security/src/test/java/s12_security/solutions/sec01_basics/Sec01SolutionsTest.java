package s12_security.solutions.sec01_basics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy rozwiązań wzorcowych lekcji Sec01Basics. @Tag("wzorzec") — biegną w domyślnym ./mvnw test.
 * {@code @WebMvcTest(ExerciseController.class)} znajduje Sec01SolutionsApplication; ExerciseSecurityConfig rozwiązania jest
 * skanowaną klasą {@code @Configuration}, więc trzeba go dołączyć — tu przez {@code @Import} na klasie testu.
 */
@Tag("wzorzec")
@WebMvcTest(ExerciseController.class)
@Import(ExerciseSecurityConfig.class)
class Sec01SolutionsTest {

    @Autowired
    MockMvcTester mvc;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String name, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                name, null, AuthorityUtils.createAuthorityList(authorities)));
    }

    @Test
    @WithMockUser(username = "ola")
    @DisplayName("Wzorzec 1: /sec01/cw/greeting wita zalogowanego użytkownika po nazwie")
    void exercise1() {
        assertThat(mvc.get().uri("/sec01/cw/greeting")).hasStatusOk().hasBodyTextEqualTo("Dzień dobry, ola");
    }

    @Test
    @DisplayName("Wzorzec 2: CurrentUser.name() — nazwa z kontekstu, a bez logowania i dla anonima \"anonim\"")
    void exercise2() {
        assertThat(CurrentUser.name()).isEqualTo("anonim");
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "klucz", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(CurrentUser.name()).isEqualTo("anonim");
        loginAs("piotr", "ROLE_CUSTOMER");
        assertThat(CurrentUser.name()).isEqualTo("piotr");
    }

    @Test
    @DisplayName("Wzorzec 3: CurrentUser.hasRole(\"ADMIN\") sprawdza uprawnienie ROLE_ADMIN")
    void exercise3() {
        assertThat(CurrentUser.hasRole("ADMIN")).isFalse();
        loginAs("ewa", "ROLE_EMPLOYEE", "FACTOR_PASSWORD");
        assertThat(CurrentUser.hasRole("EMPLOYEE")).isTrue();
        assertThat(CurrentUser.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 4: StatusRules.statusFor — 401 dla nieznanego, 403 bez roli, 200 z rolą")
    void exercise4() {
        Authentication anonymous = new AnonymousAuthenticationToken("klucz", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        Authentication customer = UsernamePasswordAuthenticationToken.authenticated("anna", null,
                AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
        Authentication notYetChecked = UsernamePasswordAuthenticationToken.unauthenticated("anna", "haslo-testowe");
        Authentication admin = UsernamePasswordAuthenticationToken.authenticated("admin", null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        assertThat(StatusRules.statusFor(null, "ADMIN")).isEqualTo(401);
        assertThat(StatusRules.statusFor(anonymous, "ADMIN")).isEqualTo(401);
        assertThat(StatusRules.statusFor(notYetChecked, "ADMIN")).isEqualTo(401);
        assertThat(StatusRules.statusFor(customer, "ADMIN")).isEqualTo(403);
        assertThat(StatusRules.statusFor(admin, "ADMIN")).isEqualTo(200);
    }

    @Test
    @DisplayName("Wzorzec 5: reguły /sec01/cw/** — publiczne 200, admin: anonim 401")
    void exercise5Anonymous() {
        assertThat(mvc.get().uri("/sec01/cw/public/info")).hasStatusOk().hasBodyTextEqualTo("Sklep czynny 8–20");
        assertThat(mvc.get().uri("/sec01/cw/admin/stats")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Wzorzec 5: /sec01/cw/admin/** — klient 403")
    void exercise5Customer() {
        assertThat(mvc.get().uri("/sec01/cw/admin/stats")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Wzorzec 5: /sec01/cw/admin/** — administrator 200")
    void exercise5Admin() {
        assertThat(mvc.get().uri("/sec01/cw/admin/stats")).hasStatusOk();
    }
}
