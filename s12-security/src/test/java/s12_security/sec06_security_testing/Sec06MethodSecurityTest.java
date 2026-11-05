package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 3 lekcji Sec06SecurityTesting: testy bezpieczeństwa METOD bez HTTP. Najmniejszy możliwy kontekst
 * ({@code @SpringJUnitConfig}): OrderService + {@link MethodSecurityTestConfig} — start w ułamku sekundy, a proxy
 * bezpieczeństwa jest prawdziwe. {@code @WithMockUser} działa tak samo jak w testach webowych.
 */
@SpringJUnitConfig(classes = {OrderService.class, MethodSecurityTestConfig.class})
class Sec06MethodSecurityTest {

    @Autowired
    OrderService orders;

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Sekcja 3: własne zamówienia tak, cudze — AuthorizationDeniedException (przez HTTP byłoby z tego 403)")
    void deniedForOtherCustomer() {
        assertThat(orders.ordersOf("anna")).hasSize(2);
        assertThatThrownBy(() -> orders.ordersOf("piotr"))
                .isInstanceOf(AuthorizationDeniedException.class)
                .hasMessage("Access Denied");
    }

    @Test
    @WithMockUser(username = "boss", roles = "ADMIN")
    @DisplayName("Sekcja 3: hierarchia ról działa też w tym małym kontekście — ADMIN widzi cudze zamówienia i anuluje")
    void adminThroughHierarchy() {
        assertThat(orders.ordersOf("piotr")).hasSize(1);
        assertThat(orders.cancel(3)).isEqualTo("Anulowano zamówienie 3");
    }

    @Test
    @DisplayName("Sekcja 3: bez użytkownika w kontekście — AuthenticationCredentialsNotFoundException (nie AccessDenied!)")
    void noUser() {
        assertThatThrownBy(() -> orders.cancel(1)).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("Sekcja 3 (pułapka): new OrderService() to obiekt BEZ proxy — anonim anuluje zamówienie, test \"przechodzi\"")
    void newBypassesSecurity() {
        OrderService withoutProxy = new OrderService();
        assertThat(withoutProxy.cancel(1)).isEqualTo("Anulowano zamówienie 1");    // żadnej reguły nikt nie sprawdził
        assertThat(orders.getClass().getName()).contains("$$SpringCGLIB$$");          // bean z kontekstu — proxy
    }
}
