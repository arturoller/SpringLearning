package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 1 lekcji Sec06SecurityTesting — PUŁAPKA: {@code @WebMvcTest} BEZ {@code @Import(Sec06SecurityConfig.class)}.
 *
 * <p>Wycinek webowy ładuje kontrolery, ale NIE ładuje klas {@code @Configuration} z pakietu — więc naszego łańcucha
 * też nie. Security i tak działa (dzięki spring-boot-starter-security-test), tyle że z DOMYŚLNYM łańcuchem Boota:
 * wszystko wymaga logowania. Test „publicznej” listy produktów dostaje 401.</p>
 */
@WebMvcTest(ShopController.class)
class Sec06SecurityTestingNoImportTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @MockitoBean   // atrapa serwisu — @WebMvcTest nie tworzy beanów @Service (s11_testing/Test02MockitoBeans)
    OrderService orderService;

    @Test
    @DisplayName("1. PUŁAPKA: bez @Import jest tylko domyślny łańcuch Boota — publiczne GET /api/products daje 401")
    void ownSecurityConfigIsNotLoaded() throws Exception {
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).containsOnlyKeys("defaultSecurityFilterChain");
        assertThat(context.getBeanNamesForType(Sec06SecurityConfig.class)).isEmpty();

        mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());   // w aplikacji: 200 (permitAll)
    }
}
