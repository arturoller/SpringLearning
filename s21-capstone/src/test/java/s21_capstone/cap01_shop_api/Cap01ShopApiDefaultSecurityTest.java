package s21_capstone.cap01_shop_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import s21_capstone.cap01_shop_api.product.ProductController;
import s21_capstone.cap01_shop_api.product.ProductService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 7 lekcji Cap01ShopApi: co by było BEZ naszego {@code SecurityFilterChain}? Wycinek {@code @WebMvcTest}
 * BEZ {@code @Import(Cap01Config.class)} nie widzi naszej konfiguracji, więc działa domyślna autokonfiguracja
 * Spring Security (starter jest na ścieżce klas całego modułu) — dokładnie tak, jak działałaby cała aplikacja,
 * gdybyśmy zapomnieli o beanie z {@code permitAll()}.
 */
@WebMvcTest(ProductController.class)
class Cap01ShopApiDefaultSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService productService;

    @Test
    @DisplayName("7. Bez własnego łańcucha: GET → 401 (wymagane logowanie), POST → 403 (brak tokenu CSRF)")
    void defaultSecurityBlocksEverything() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
