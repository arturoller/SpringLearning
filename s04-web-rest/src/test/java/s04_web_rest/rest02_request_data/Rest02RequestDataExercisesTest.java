package s04_web_rest.rest02_request_data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy ĆWICZEŃ lekcji Rest02RequestData. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@WebMvcTest(ShopExerciseController.class)
class Rest02RequestDataExercisesTest {

    static final String CART = """
            {"lines":[{"priceGrosze":1000,"quantity":2},{"priceGrosze":999,"quantity":1}]}""";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: powitanie z parametru name i domyślne „gościu”")
    void exercise1() throws Exception {
        mockMvc.perform(get("/api/exercise/greeting").param("name", "Ala"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"text\":\"Cześć, Ala!\"}"));
        mockMvc.perform(get("/api/exercise/greeting"))
                .andExpect(content().json("{\"text\":\"Cześć, gościu!\"}"));
    }

    @Test
    @DisplayName("Ćwiczenie 2: kategoria ze ścieżki, lista tagów i limit z wartością domyślną")
    void exercise2() throws Exception {
        mockMvc.perform(get("/api/exercise/categories/kawa/products?tag=bio&tag=fair&limit=5"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"category\":\"kawa\",\"tags\":[\"bio\",\"fair\"],\"limit\":5}"));
        mockMvc.perform(get("/api/exercise/categories/herbata/products"))
                .andExpect(content().json("{\"category\":\"herbata\",\"tags\":[],\"limit\":10}"));
        mockMvc.perform(get("/api/exercise/categories/kawa/products?limit=abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ćwiczenie 3: suma koszyka z ciała JSON i rabat z nagłówka; błędne dane → 400, bez Content-Type → 415")
    void exercise3() throws Exception {
        mockMvc.perform(post("/api/exercise/cart/total").contentType(MediaType.APPLICATION_JSON).content(CART))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"totalGrosze\":2999,\"discountPercent\":0}"));
        mockMvc.perform(post("/api/exercise/cart/total").contentType(MediaType.APPLICATION_JSON).content(CART)
                        .header("X-Discount-Percent", "10"))
                .andExpect(content().json("{\"totalGrosze\":2699,\"discountPercent\":10}"));
        mockMvc.perform(post("/api/exercise/cart/total").contentType(MediaType.APPLICATION_JSON).content(CART)
                        .header("X-Discount-Percent", "150"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/exercise/cart/total").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/exercise/cart/total").content(CART))
                .andExpect(status().isUnsupportedMediaType());
    }
}
