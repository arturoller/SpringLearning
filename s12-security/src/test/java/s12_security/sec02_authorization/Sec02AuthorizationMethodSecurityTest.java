package s12_security.sec02_authorization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy sekcji 4 lekcji Sec02Authorization — {@code @PreAuthorize} bez Spring MVC i bez HTTP.
 *
 * <p>{@code ApplicationContextRunner} (uruchamiacz kontekstu z Boot Test) buduje mały kontekst tylko z podanych klas —
 * idealny do porównania „z adnotacją” i „bez adnotacji”. Zalogowanego użytkownika ustawiamy ręcznie
 * w {@code SecurityContextHolder} (więcej o nim: Sec03UsersPasswords).</p>
 */
class Sec02AuthorizationMethodSecurityTest {

    /** Konfiguracja, która TYLKO włącza zabezpieczenia metod. */
    @EnableMethodSecurity
    static class MethodSecurityOn {
    }

    static void loginAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                username, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @AfterEach   // after each = po każdym teście — sprzątamy, żeby zalogowany użytkownik nie „przeciekł” do innego testu
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("4. PUŁAPKA: bez @EnableMethodSecurity @PreAuthorize nic nie robi — piotr czyta zamówienia anny")
    void withoutEnableMethodSecurity() {
        new ApplicationContextRunner().withUserConfiguration(OrderService.class).run(context -> {
            OrderService service = context.getBean(OrderService.class);
            assertThat(AopUtils.isAopProxy(service)).as("zwykły obiekt, bez proxy").isFalse();

            loginAs("piotr", "USER");
            assertThat(service.ordersOf("anna")).hasSize(1);   // powinno być zabronione — a nie jest
        });
    }

    @Test
    @DisplayName("4. Z @EnableMethodSecurity bean jest proxy, a odmowa to AuthorizationDeniedException(\"Access Denied\")")
    void withEnableMethodSecurity() {
        new ApplicationContextRunner().withUserConfiguration(MethodSecurityOn.class, OrderService.class).run(context -> {
            OrderService service = context.getBean(OrderService.class);
            assertThat(AopUtils.isAopProxy(service)).as("Spring podmienił bean na proxy").isTrue();

            loginAs("piotr", "USER");
            assertThat(service.ordersOf("piotr")).hasSize(2);
            assertThatThrownBy(() -> service.ordersOf("anna"))
                    .isInstanceOf(AuthorizationDeniedException.class)
                    .hasMessage("Access Denied");
        });
    }
}
