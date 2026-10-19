package s12_security.solutions.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import s12_security.sec06_security_testing.OrderService;
import s12_security.sec06_security_testing.Sec06Application;
import s12_security.sec06_security_testing.Sec06SecurityConfig;
import s12_security.sec06_security_testing.ShopController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Sec06SecurityTesting — to są gotowe testy. {@code @Tag("wzorzec")} — biegną zawsze.
 *
 * <p>Ten pakiet (solutions) nie leży pod pakietem lekcji, więc {@code @WebMvcTest} nie znalazłby sam klasy
 * {@code @SpringBootApplication} („Unable to find a @SpringBootConfiguration”). {@code @ContextConfiguration}
 * wskazuje ją jawnie.</p>
 */
@Tag("wzorzec")
@WebMvcTest(ShopController.class)
@ContextConfiguration(classes = Sec06Application.class)
@Import(Sec06SecurityConfig.class)
class Sec06SecurityTestingSolutionsTest {

    static final String PRODUCT = "{\"sku\":\"ODZ-003\",\"name\":\"Czapka zimowa\"}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @Test
    @DisplayName("Wzorzec 1: niezalogowany GET /api/orders/piotr → 401")
    void solution1() throws Exception {
        mockMvc.perform(get("/api/orders/piotr")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Wzorzec 2: admin + csrf() → 201; admin bez csrf() → 403")
    void solution2() throws Exception {
        mockMvc.perform(post("/api/products").with(user("anna").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/products").with(user("anna").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Wzorzec 3: jwt() ze scope stock:read → 200 i partner z sub; scope orders:read → 403")
    void solution3() throws Exception {
        mockMvc.perform(get("/api/partner/stock").with(jwt().jwt(token -> token
                        .subject("hurtownia-xyz").claim("scope", "stock:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partner").value("hurtownia-xyz"));
        mockMvc.perform(get("/api/partner/stock").with(jwt().jwt(token -> token
                        .subject("hurtownia-xyz").claim("scope", "orders:read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @SolutionWithMockAdmin
    @DisplayName("Wzorzec 4: @SolutionWithMockAdmin = anna z rolą ADMIN")
    void solution4() throws Exception {
        mockMvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/me")).andExpect(jsonPath("$.username").value("anna"));
    }
}
