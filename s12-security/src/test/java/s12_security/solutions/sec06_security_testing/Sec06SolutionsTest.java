package s12_security.solutions.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import s12_security.sec06_security_testing.OrderService;
import s12_security.sec06_security_testing.Sec06Application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Testy rozwiązań wzorcowych ćwiczeń 1–4 lekcji Sec06SecurityTesting. @Tag("wzorzec") — biegną w domyślnym ./mvnw test.
 * Aplikacja ta sama co w lekcji ({@code classes = Sec06Application.class}); rozwiązania to klasy pomocnicze testów.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Sec06Application.class)
@AutoConfigureMockMvc
class Sec06SolutionsTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    OrderService orders;

    private static RequestPostProcessor who(String who) {
        return switch (who) {
            case "anonim" -> anonymous();
            case "anna" -> user("anna").roles("CUSTOMER");
            case "ewa" -> user("ewa").roles("EMPLOYEE");
            default -> user("admin").roles("ADMIN");
        };
    }

    @Test
    @DisplayName("Wzorzec 1: TestUsers.customer(\"piotr\") widzi swoje zamówienia, ale nie cudze; adminToken() wchodzi do panelu admina")
    void exercise1() {
        assertThat(mvc.get().uri("/sec06/orders/of/piotr").with(TestUsers.customer("piotr"))).hasStatusOk();
        assertThat(mvc.get().uri("/sec06/orders/of/anna").with(TestUsers.customer("piotr"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec06/admin/stats").with(TestUsers.adminToken())).hasStatusOk();
    }

    @ParameterizedTest(name = "{0}, token CSRF: {1}")
    @CsvSource({"anonim, false", "anonim, true", "anna, false", "anna, true",
            "ewa, false", "ewa, true", "admin, false", "admin, true"})
    @DisplayName("Wzorzec 2: przewidziany status POST /sec06/panel/price zgadza się z aplikacją")
    void exercise2(String who, boolean withCsrf) {
        int expected = PanelRules.expectedStatus(who, withCsrf);
        RequestPostProcessor token = withCsrf ? csrf() : request -> request;
        assertThat(mvc.post().uri("/sec06/panel/price").param("product", "kawa").param("price", "41.90")
                .with(who(who)).with(token))
                .hasStatus(expected);
    }

    @Test
    @DisplayName("Wzorzec 3: bearerFor daje działający nagłówek — ewa widzi zamówienia piotra, anna dostaje 403")
    void exercise3() {
        String ewa = ExerciseTokens.bearerFor(mvc, "ewa");
        assertThat(ewa).startsWith("Bearer ey");
        assertThat(mvc.get().uri("/sec06/orders/of/piotr").header(HttpHeaders.AUTHORIZATION, ewa)).hasStatusOk();
        assertThat(mvc.get().uri("/sec06/orders/of/piotr").header(HttpHeaders.AUTHORIZATION, ExerciseTokens.bearerFor(mvc, "anna")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Wzorzec 4: runAs ustawia użytkownika na czas akcji i ZAWSZE sprząta kontekst — także po odmowie dostępu")
    void exercise4() {
        assertThat(ExerciseSecurityContext.runAs("anna", "CUSTOMER", () -> orders.ordersOf("anna"))).hasSize(2);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThatThrownBy(() -> ExerciseSecurityContext.runAs("anna", "CUSTOMER", () -> orders.ordersOf("piotr")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(ExerciseSecurityContext.runAs("boss", "ADMIN", () -> orders.cancel(2))).isEqualTo("Anulowano zamówienie 2");
    }
}
