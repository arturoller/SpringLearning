package s06_error_handling.err02_exception_handler;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 1-4 lekcji Err02ExceptionHandler. Najwęższy wycinek: {@code @WebMvcTest(ShopController.class)} — własny handler sam
 * buduje ciało odpowiedzi, więc MockMvc je widzi (inaczej niż domyślne ciało z {@code /error}, patrz Err01DefaultErrors).
 */
@WebMvcTest(ShopController.class)
class Err02ExceptionHandlerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 1: handler w kontrolerze zamienia ProductNotFoundException na 404 z własnym ciałem ApiError")
    void handlerBuildsOwnBody() throws Exception {
        mockMvc.perform(get("/err02/shop/products/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nie ma produktu o id 9"))
                .andExpect(jsonPath("$.path").value("/err02/shop/products/9"));
        mockMvc.perform(get("/err02/shop/products/1")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Kawa"));
    }

    @Test
    @DisplayName("Sekcja 2: handler może zwracać sam rekord ze statusem z @ResponseStatus i brać WebRequest zamiast HttpServletRequest")
    void returnTypesAndParameters() throws Exception {
        mockMvc.perform(post("/err02/shop/pay").param("card", "0000"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("CARD_DECLINED"))
                .andExpect(jsonPath("$.path").value("/err02/shop/pay"));
    }

    @Test
    @DisplayName("Sekcja 2 (pułapka): handler zwracający sam rekord bez @ResponseStatus odpowiada 200 OK z opisem błędu w ciele")
    void forgottenStatusMeans200() throws Exception {
        mockMvc.perform(get("/err02/shop/coupon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(410))   // w ciele jest 410, ale kod odpowiedzi to 200
                .andExpect(jsonPath("$.code").value("COUPON_EXPIRED"));
    }

    @Test
    @DisplayName("Sekcja 3: kilka typów w jednej adnotacji — oba wyjątki magazynu dają 409, ale z różnym kodem")
    void severalTypesInOneAnnotation() throws Exception {
        mockMvc.perform(post("/err02/shop/products/1/reserve")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK")).andExpect(jsonPath("$.message").value("Brak towaru: Kawa"));
        mockMvc.perform(post("/err02/shop/products/2/reserve")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WAREHOUSE_CLOSED"));
    }

    @Test
    @DisplayName("Sekcja 3: wygrywa NAJBLIŻSZY typ w hierarchii — CardDeclined ma swój handler, PaymentException ma ogólniejszy")
    void nearestTypeWins() throws Exception {
        mockMvc.perform(post("/err02/shop/pay").param("card", "0000")).andExpect(jsonPath("$.code").value("CARD_DECLINED"));
        mockMvc.perform(post("/err02/shop/pay").param("card", "1111"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PAYMENT_ERROR"));
        mockMvc.perform(post("/err02/shop/pay").param("card", "4242")).andExpect(status().isOk()).andExpect(content().string("Zapłacono"));
    }

    @Test
    @DisplayName("Sekcja 3: handler dla RuntimeException łapie resztę, a klient nie widzi szczegółów wyjątku")
    void catchAllForTheController() throws Exception {
        mockMvc.perform(get("/err02/shop/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("UNEXPECTED"))
                .andExpect(jsonPath("$.message").value("Wystąpił nieoczekiwany błąd"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hasło"))));
        // bliższy handler nadal wygrywa z RuntimeException, mimo że ProductNotFoundException też jest RuntimeException
        mockMvc.perform(get("/err02/shop/products/9")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sekcja 3 (pułapka): handler RuntimeException połyka też 400 od Springa — zepsuty JSON daje 500")
    void catchAllSwallowsSpringsOwn400() throws Exception {
        mockMvc.perform(post("/err02/shop/products").contentType(JSON).content("{zepsuty json"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("UNEXPECTED"));
    }

    @Test
    @DisplayName("Sekcja 4: błąd walidacji ciała → 400 i mapa pole → komunikat (wyjątek MethodArgumentNotValidException)")
    void validationBodyToSortedMap() throws Exception {
        var result = mockMvc.perform(post("/err02/shop/products").contentType(JSON).content("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Nazwa jest wymagana"))
                .andExpect(jsonPath("$.quantity").value("Ilość musi wynosić co najmniej 1"))
                .andReturn();

        assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class);
        String json = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json.indexOf("\"name\"")).isLessThan(json.indexOf("\"quantity\""));   // posortowane
    }

    @Test
    @DisplayName("Sekcja 4: ograniczenie na parametrze zmienia wyjątek na HandlerMethodValidationException — potrzebny drugi handler")
    void validationOnParameterNeedsSecondHandler() throws Exception {
        var result = mockMvc.perform(put("/err02/shop/products/0").contentType(JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.id").value("Id musi być dodatnie"))
                .andExpect(jsonPath("$.name").value("Nazwa jest wymagana"))
                .andReturn();

        assertThat(result.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);
    }

    @Test
    @DisplayName("Sekcja 5 (pułapka): wyjątek rzucony w samym handlerze — klient i tak nie dostaje obsługi, a test widzi oryginał")
    void exceptionInsideHandler() {
        assertThatThrownBy(() -> mockMvc.perform(get("/err02/shop/products/3")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(BrokenDataException.class);   // oryginalny wyjątek, nie "błąd w samym handlerze"
    }
}
