package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ĆWICZENIA lekcji Sec06SecurityTesting — tym razem TY piszesz testy. {@code @Tag("cwiczenie")} — uruchamiasz sam.
 * Ćwiczenia 1–3: zamiast „throw” napisz test (wzór: Sec06SecurityTestingTest). Ćwiczenie 4: uzupełnij adnotację
 * WithMockAdmin — test jest gotowy.
 * Sprawdź swój test „na odwrót”: zmień oczekiwany status (np. 401 na 200) — test MUSI wtedy paść. Test, który
 * przechodzi zawsze, niczego nie sprawdza.
 */
@Tag("cwiczenie")
@WebMvcTest(ShopController.class)
@Import(Sec06SecurityConfig.class)
class Sec06SecurityTestingExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @Test
    @DisplayName("Ćwiczenie 1 (łatwe): niezalogowany GET /api/orders/piotr → 401")
    void exercise1() throws Exception {
        // TODO: twoje rozwiązanie — mockMvc.perform(get(...)).andExpect(status()...)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 2 (średnie): user(\"anna\").roles(\"ADMIN\") + csrf() → POST /api/products 201; bez csrf() → 403")
    void exercise2() throws Exception {
        // TODO: twoje rozwiązanie — dwa zapytania POST z JSON-em produktu, .with(user(...)) i .with(csrf())
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3 (średnie): jwt() z sub „hurtownia-xyz” i scope stock:read → 200 (partner = hurtownia-xyz); scope orders:read → 403")
    void exercise3() throws Exception {
        // TODO: twoje rozwiązanie — jwt().jwt(token -> token.subject(...).claim("scope", ...)), jsonPath("$.partner")
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @WithMockAdmin
    @DisplayName("Ćwiczenie 4 (trudniejsze): własna adnotacja @WithMockAdmin = anna z rolą ADMIN (uzupełnij WithMockAdmin)")
    void exercise4() throws Exception {
        mockMvc.perform(post("/api/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"ODZ-003\",\"name\":\"Czapka zimowa\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/me")).andExpect(jsonPath("$.username").value("anna"));
    }
}
