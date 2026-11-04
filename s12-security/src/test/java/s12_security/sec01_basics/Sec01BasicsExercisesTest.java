package s12_security.sec01_basics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ 1–4 lekcji Sec01Basics. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Ćwiczenie 5: Sec01ExerciseSecurityTest.
 * Ćwiczenie 1 korzysta z reguł lekcji ({@code @Import(Sec01SecurityConfig.class)}); ćwiczenia 2–4 to zwykłe klasy.
 */
@Tag("cwiczenie")
@WebMvcTest(ExerciseController.class)
@Import(Sec01SecurityConfig.class)
class Sec01BasicsExercisesTest {

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
    @DisplayName("Ćwiczenie 1: /sec01/cw/greeting wita zalogowanego użytkownika po nazwie")
    void exercise1() {
        assertThat(mvc.get().uri("/sec01/cw/greeting")).hasStatusOk().hasBodyTextEqualTo("Dzień dobry, ola");
    }

    @Test
    @DisplayName("Ćwiczenie 2: CurrentUser.name() — nazwa z kontekstu, a bez logowania i dla anonima \"anonim\"")
    void exercise2() {
        assertThat(CurrentUser.name()).isEqualTo("anonim");
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "klucz", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(CurrentUser.name()).isEqualTo("anonim");
        loginAs("piotr", "ROLE_CUSTOMER");
        assertThat(CurrentUser.name()).isEqualTo("piotr");
    }

    @Test
    @DisplayName("Ćwiczenie 3: CurrentUser.hasRole(\"ADMIN\") sprawdza uprawnienie ROLE_ADMIN")
    void exercise3() {
        assertThat(CurrentUser.hasRole("ADMIN")).isFalse();
        loginAs("ewa", "ROLE_EMPLOYEE", "FACTOR_PASSWORD");
        assertThat(CurrentUser.hasRole("EMPLOYEE")).isTrue();
        assertThat(CurrentUser.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 4: StatusRules.statusFor — 401 dla nieznanego, 403 bez roli, 200 z rolą")
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
}
