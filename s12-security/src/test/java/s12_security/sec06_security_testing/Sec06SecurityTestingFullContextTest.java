package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Sec06SecurityTesting, sekcja 6: {@code @SpringBootTest} + {@code @AutoConfigureMockMvc} — PEŁNY kontekst.
 * Konfiguracja bezpieczeństwa ładuje się sama (bez {@code @Import}), a OrderService jest prawdziwym beanem z proxy
 * {@code @PreAuthorize}. {@code @WithMockUser} działa też bez MockMvc — przy zwykłym wywołaniu metody serwisu.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sec06SecurityTestingFullContextTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OrderService orderService;

    @Test
    @DisplayName("6. Pełny kontekst: nasza konfiguracja jest bez @Import; prawdziwy serwis: piotr nie widzi zamówień anny")
    void fullContextLoadsEverything() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/piotr").with(httpBasic("piotr", "piotr-test-123")))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"ZAM-002\",\"ZAM-003\"]"));
        mockMvc.perform(get("/api/orders/anna").with(httpBasic("piotr", "piotr-test-123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "piotr")
    @DisplayName("6. @WithMockUser na teście serwisu: swoje zamówienia → lista, cudze → AuthorizationDeniedException")
    void methodSecurityWithMockUser() {
        assertThat(orderService.ordersOf("piotr")).containsExactly("ZAM-002", "ZAM-003");
        assertThatThrownBy(() -> orderService.ordersOf("anna")).isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "zofia", roles = "ADMIN")
    @DisplayName("6. Admin (zofia — nie musi istnieć!) widzi cudze zamówienia")
    void adminSeesEverything() {
        assertThat(orderService.ordersOf("anna")).containsExactly("ZAM-001");
    }

    @Test
    @DisplayName("6. PUŁAPKA: bez żadnego użytkownika wyrażenie SpEL się nie liczy — IllegalArgumentException, w środku brak Authentication")
    void noUserAtAll() {
        assertThatThrownBy(() -> orderService.ordersOf("piotr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to evaluate expression '#owner == authentication.name or hasRole('ADMIN')'")
                .rootCause()
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("An Authentication object was not found in the SecurityContext");
    }
}
