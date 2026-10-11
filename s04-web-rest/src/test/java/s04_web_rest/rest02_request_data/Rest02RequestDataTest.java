package s04_web_rest.rest02_request_data;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest02RequestData. {@code @WebMvcTest(ProductQueryController.class)} — tylko ten kontroler i
 * infrastruktura MVC (konwersja typów, Jackson); nic więcej nie jest potrzebne.
 * {@code result.getResolvedException()} pokazuje, JAKI wyjątek Spring zamienił na kod 4xx.
 */
@WebMvcTest(ProductQueryController.class)
class Rest02RequestDataTest {

    static final String ORDER_JSON = """
            {"customer":"Anna","lines":[{"productId":1,"quantity":2},{"productId":2,"quantity":3}]}""";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. @PathVariable: liczba z adresu trafia do parametru long; dwie zmienne i jawna nazwa")
    void pathVariable() throws Exception {
        mockMvc.perform(get("/api/products/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
        mockMvc.perform(get("/api/categories/kawa/products/7"))
                .andExpect(jsonPath("$.category").value("kawa"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("1. @PathVariable „abc” zamiast liczby → 400 (MethodArgumentTypeMismatchException) — pytanie 2")
    void pathVariableConversionFails() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MethodArgumentTypeMismatchException.class));
    }

    @Test
    @DisplayName("2. @RequestParam: tylko wymagany name → nieobowiązkowe null, domyślne 0 i 20, pusta lista tagów")
    void requestParamDefaults() throws Exception {
        mockMvc.perform(get("/api/search").param("name", "kawa"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"name":"kawa","maxPrice":null,"minPrice":null,"page":0,"size":20,"tags":[]}"""));
    }

    @Test
    @DisplayName("2. @RequestParam: wszystkie parametry, tag dwa razy → lista; tag=bio,fair → też lista (pytanie 5)")
    void requestParamAll() throws Exception {
        mockMvc.perform(get("/api/search?name=kawa&maxPrice=3000&minPrice=100&page=2&tag=bio&tag=fair"))
                .andExpect(content().json("""
                        {"name":"kawa","maxPrice":3000,"minPrice":100,"page":2,"size":20,"tags":["bio","fair"]}"""));
        mockMvc.perform(get("/api/search?name=kawa&tag=bio,fair"))
                .andExpect(jsonPath("$.tags[0]").value("bio"))
                .andExpect(jsonPath("$.tags[1]").value("fair"));
    }

    @Test
    @DisplayName("2. Brak wymaganego name → 400; zły typ page → 400; pusty name → 200 (pytanie 7)")
    void requestParamErrors() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingServletRequestParameterException.class));
        mockMvc.perform(get("/api/search?name=kawa&page=x")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/search?name="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(""));
    }

    @Test
    @DisplayName("2. ZŁY PRZYKŁAD: required = false na int i brak parametru → IllegalStateException (500), nie 400 (pytanie 3)")
    void primitiveOptionalParam() throws Exception {
        mockMvc.perform(get("/api/search/limited?limit=5")).andExpect(content().string("5"));

        // MockMvc nie ma serwera, który zamieniłby nieobsłużony wyjątek na 500 — wyjątek „wylatuje” z perform(...)
        assertThatThrownBy(() -> mockMvc.perform(get("/api/search/limited")))
                .isInstanceOf(ServletException.class)
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be translated into a null value");
    }

    @Test
    @DisplayName("3. @RequestHeader: wartość z nagłówka (wielkość liter nazwy bez znaczenia), domyślny język „pl”")
    void requestHeader() throws Exception {
        mockMvc.perform(get("/api/client-info").header("x-client-id", "app-7"))
                .andExpect(content().json("""
                        {"clientId":"app-7","language":"pl"}"""));
        mockMvc.perform(get("/api/client-info").header("X-Client-Id", "app-7").header("Accept-Language", "en"))
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    @DisplayName("3. Brak wymaganego nagłówka → 400 (MissingRequestHeaderException)")
    void missingHeader() throws Exception {
        mockMvc.perform(get("/api/client-info"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(MissingRequestHeaderException.class));
    }

    @Test
    @DisplayName("4. @RequestBody: JSON → rekord NewOrder z listą pozycji")
    void requestBody() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ORDER_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"customer":"Anna","lineCount":2,"totalQuantity":5}"""));
    }

    @Test
    @DisplayName("4. PUŁAPKA: JSON bez Content-Type albo jako text/plain → 415 Unsupported Media Type (pytanie 4)")
    void missingContentType() throws Exception {
        mockMvc.perform(post("/api/orders").content(ORDER_JSON))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(HttpMediaTypeNotSupportedException.class)
                        .hasMessageContaining("application/octet-stream"));
        mockMvc.perform(post("/api/orders").contentType(MediaType.TEXT_PLAIN).content(ORDER_JSON))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("4. Uszkodzony JSON, brak ciała, brak pola int → 400; nieznane pole → 200 (zignorowane)")
    void requestBodyErrors() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{\"customer\":"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\":\"A\",\"lines\":[{\"productId\":1}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\":\"A\",\"vip\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineCount").value(0));
    }
}
