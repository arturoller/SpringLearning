package s12_security.sec02_authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Sec02Authorization (sekcje 1, 3, 4, 5; sekcja 2 — Sec02AuthorizationRuleOrderTest).
 *
 * <p>{@code @SpringBootTest} + {@code @AutoConfigureMockMvc} — pełny kontekst (łańcuch filtrów, użytkownicy,
 * OrderService z {@code @PreAuthorize}) i MockMvc bez sieci. Boot sam dokłada filtry Security do MockMvc.
 * Nagłówek Authorization piszemy ręcznie, żeby było widać, co leci do serwera; wygodniejsze sposoby
 * ({@code httpBasic()}, {@code @WithMockUser}) — Sec06SecurityTesting.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc   // auto configure MockMvc = skonfiguruj MockMvc automatycznie (razem z filtrami Security)
class Sec02AuthorizationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserDetailsService users;

    /** Dokleja nagłówek HTTP Basic dla użytkownika (hasło testowe = login + "-test-123"). */
    static MockHttpServletRequestBuilder as(String user, MockHttpServletRequestBuilder request) {
        String credentials = user + ":" + user + "-test-123";
        return request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
    }

    static List<String> authorityNames(UserDetails user) {
        return user.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
    }

    @Test
    @DisplayName("1. roles(\"ADMIN\") zapisuje uprawnienie ROLE_ADMIN; tomasz ma rolę i zwykłe uprawnienie report:read")
    void rolesAreAuthoritiesWithPrefix() {
        assertThat(authorityNames(users.loadUserByUsername("anna"))).containsExactly("ROLE_ADMIN");
        assertThat(authorityNames(users.loadUserByUsername("kasia"))).containsExactly("ROLE_HR");
        assertThat(authorityNames(users.loadUserByUsername("tomasz"))).containsExactly("ROLE_SALES", "report:read");
    }

    @Test
    @DisplayName("1. PUŁAPKA: roles() i authorities() nadpisują się — zostaje tylko ostatnie; roles(\"ROLE_X\") to błąd")
    void rolesAndAuthoritiesOverwriteEachOther() {
        UserDetails overwritten = User.withUsername("tomasz").password("{noop}x")
                .roles("SALES")                    // ROLE_SALES...
                .authorities("report:read")        // ...zastąpione przez report:read
                .build();
        assertThat(authorityNames(overwritten)).containsExactly("report:read");

        assertThatThrownBy(() -> User.withUsername("anna").password("{noop}x").roles("ROLE_ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ROLE_ADMIN cannot start with ROLE_ (it is automatically added)");
    }

    @Test
    @DisplayName("1. hasAnyRole(\"HR\", \"ADMIN\"): kasia i anna → 200, piotr → 403")
    void hasAnyRole() throws Exception {
        mockMvc.perform(as("kasia", get("/api/hr/employees"))).andExpect(status().isOk());
        mockMvc.perform(as("anna", get("/api/hr/employees"))).andExpect(status().isOk());
        mockMvc.perform(as("piotr", get("/api/hr/employees"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("1. hasAuthority(\"report:read\"): tomasz → 200, a anna (ADMIN) → 403 — role nie tworzą hierarchii")
    void hasAuthority() throws Exception {
        mockMvc.perform(as("tomasz", get("/api/reports/sales"))).andExpect(status().isOk());
        mockMvc.perform(as("anna", get("/api/reports/sales"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Metody HTTP: GET produktów — każdy; POST — tylko ADMIN (piotr 403, anna 201); DELETE anonima → 401")
    void httpMethodsInRules() throws Exception {
        String product = "{\"sku\":\"DOM-002\",\"name\":\"Lampka biurkowa\",\"price\":129.00}";

        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(as("piotr", post("/api/products").contentType(MediaType.APPLICATION_JSON).content(product)))
                .andExpect(status().isForbidden());
        mockMvc.perform(as("anna", post("/api/products").contentType(MediaType.APPLICATION_JSON).content(product)))
                .andExpect(status().isCreated());
        mockMvc.perform(as("anna", delete("/api/products/DOM-002"))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/products/DOM-002")).andExpect(status().isUnauthorized());
        // PUT nie ma żadnej reguły → anyRequest().denyAll() → nawet anna 403
        mockMvc.perform(as("anna", put("/api/products/DOM-002").contentType(MediaType.APPLICATION_JSON).content(product)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. @PreAuthorize z #owner: swoje zamówienia — każdy, cudze — tylko ADMIN")
    void preAuthorizeWithParameter() throws Exception {
        mockMvc.perform(as("piotr", get("/api/orders/piotr"))).andExpect(status().isOk());
        mockMvc.perform(as("piotr", get("/api/orders/anna"))).andExpect(status().isForbidden());
        mockMvc.perform(as("anna", get("/api/orders/piotr"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. @PostAuthorize sprawdza zwrócony obiekt: piotr widzi ZAM-002, ale nie ZAM-001 anny")
    void postAuthorizeChecksResult() throws Exception {
        mockMvc.perform(as("piotr", get("/api/orders/by-id/ZAM-002"))).andExpect(status().isOk());
        mockMvc.perform(as("piotr", get("/api/orders/by-id/ZAM-001"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. PUŁAPKA: wywołanie z wnętrza klasy omija proxy — piotr widzi podsumowanie zamówień anny")
    void selfInvocationBypassesSecurity() throws Exception {
        mockMvc.perform(as("piotr", get("/api/orders/anna/summary")))
                .andExpect(status().isOk())
                .andExpect(content().string("anna: 1 zamówień na 5499.99 zł"));
    }

    @Test
    @DisplayName("5. anyRequest().denyAll(): zapomniany punkt końcowy /api/stats — anna 403, anonim 401")
    void denyAllByDefault() throws Exception {
        mockMvc.perform(as("anna", get("/api/stats"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/stats")).andExpect(status().isUnauthorized());
    }
}
