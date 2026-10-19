package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Sec06SecurityTesting, sekcje 1–5: {@code @WebMvcTest} + {@code @Import(Sec06SecurityConfig.class)} —
 * wycinek webowy z NASZYM łańcuchem filtrów, użytkownikami i dekoderem JWT. Każda reguła ma test 401/403/200.
 * OrderService to atrapa ({@code @MockitoBean}); samo {@code @PreAuthorize} testuje Sec06SecurityTestingFullContextTest.
 */
@WebMvcTest(ShopController.class)
@Import(Sec06SecurityConfig.class)   // bez tego: Sec06SecurityTestingNoImportTest
class Sec06SecurityTestingTest {

    static final String PRODUCT = "{\"sku\":\"ODZ-003\",\"name\":\"Czapka zimowa\"}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @MockitoBean
    OrderService orderService;

    @Test
    @DisplayName("1. Z @Import w kontekście są nasze dwa łańcuchy, a publiczne GET /api/products daje 200")
    void importLoadsOwnChains() throws Exception {
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).containsOnlyKeys("partnerChain", "shopChain");
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser   // with anonymous user = jako niezalogowany (jawnie)
    @DisplayName("2. @WithAnonymousUser: POST /api/products → 401")
    void anonymousCannotCreate() throws Exception {
        mockMvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser   // domyślnie: użytkownik „user”, rola USER
    @DisplayName("2. @WithMockUser (user, rola USER): POST /api/products → 403, a ciało w MockMvc jest PUSTE")
    void userCannotCreate() throws Exception {
        mockMvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));   // na prawdziwym serwerze byłby JSON ze strony /error
    }

    @Test
    @WithMockUser(username = "anna", roles = "ADMIN")
    @DisplayName("2. @WithMockUser(roles = \"ADMIN\") + csrf(): POST /api/products → 201")
    void adminCanCreate() throws Exception {
        mockMvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "anna", roles = "ADMIN")
    @DisplayName("2. PUŁAPKA: admin, ale POST BEZ csrf() → 403 (to CSRF, nie rola!)")
    void adminWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "nikt-taki")
    @DisplayName("2. PUŁAPKA: @WithMockUser nie sprawdza, czy użytkownik istnieje, i pomija logowanie (brak FACTOR_PASSWORD)")
    void mockUserDoesNotNeedToExist() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"username\":\"nikt-taki\",\"authorities\":[\"ROLE_USER\"]}", true));
    }

    @Test
    @WithUserDetails("anna")   // with user details = użytkownik wczytany z beana UserDetailsService
    @DisplayName("3. @WithUserDetails(\"anna\"): prawdziwy użytkownik z UserDetailsService (rola ADMIN)")
    void withUserDetails() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(content().json("{\"username\":\"anna\",\"authorities\":[\"ROLE_ADMIN\"]}", true));
    }

    @Test
    @DisplayName("4. httpBasic(): PRAWDZIWE logowanie (hasło, PasswordEncoder) — dobre hasło 200 + FACTOR_PASSWORD, złe 401")
    void httpBasicGoesThroughRealAuthentication() throws Exception {
        mockMvc.perform(get("/api/me").with(httpBasic("anna", "anna-test-123")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"username\":\"anna\",\"authorities\":[\"FACTOR_PASSWORD\",\"ROLE_ADMIN\"]}", true));
        mockMvc.perform(get("/api/me").with(httpBasic("anna", "zle-haslo")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4. user(\"piotr\").roles(\"USER\") — użytkownik dla jednego zapytania: POST /api/products → 403")
    void userPostProcessor() throws Exception {
        mockMvc.perform(post("/api/products").with(user("piotr").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. jwt(): token bez prawdziwego podpisu — SCOPE_stock:read → 200; zwykły jwt() (SCOPE_read) → 403; brak → 401")
    void jwtPostProcessor() throws Exception {
        mockMvc.perform(get("/api/partner/stock").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_stock:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partner").value("user"));   // domyślny sub tokenu z jwt()

        mockMvc.perform(get("/api/partner/stock").with(jwt().jwt(token -> token
                        .subject("hurtownia-abc").claim("scope", "stock:read"))))   // uprawnienia ze scope, jak w aplikacji
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partner").value("hurtownia-abc"));

        mockMvc.perform(get("/api/partner/stock").with(jwt())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/partner/stock")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. Hasło nie wycieka: odpowiedź /api/me nie zawiera hasła ani skrótu BCrypt")
    void passwordDoesNotLeak() throws Exception {
        String body = mockMvc.perform(get("/api/me").with(httpBasic("anna", "anna-test-123")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("password").doesNotContain("anna-test-123").doesNotContain("$2a$");
    }

    @Test
    @WithMockUser(username = "anna", roles = "ADMIN")
    @DisplayName("5. denyAll: nawet admin dostaje 403 na adres bez reguły (/api/stats, /api/partner/inne)")
    void denyAllRules() throws Exception {
        mockMvc.perform(get("/api/stats")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/partner/inne").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_stock:read"))))
                .andExpect(status().isForbidden());
    }
}
