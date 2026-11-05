package s04_web_rest.rest02_request_data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest02RequestData. Wycinek: {@code @WebMvcTest} (kontrolery + MockMvc, bez Tomcata). Katalog produktów
 * to zwykły bean w pamięci, a @WebMvcTest nie skanuje zwykłych @Component — dlatego dodajemy go przez {@code @Import}.
 */
@WebMvcTest
@Import(ProductCatalog.class)
class Rest02RequestDataTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. @PathVariable: {id} ze ścieżki trafia do metody jako Long; brak produktu → 404")
    void pathVariable() throws Exception {
        mockMvc.perform(get("/rest02/products/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Czekolada"));

        mockMvc.perform(get("/rest02/products/999")).andExpect(status().isNotFound());

        // nazwa podana jawnie: @PathVariable("productId") Long number
        mockMvc.perform(get("/rest02/products/by-id/2"))
                .andExpect(jsonPath("$.name").value("Herbata"));
    }

    @Test
    @DisplayName("1. Zła wartość zmiennej ścieżki (abc zamiast liczby) → 400, a nie 404 ani 500")
    void pathVariableWrongType() throws Exception {
        mockMvc.perform(get("/rest02/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .hasMessageContaining("Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'"));
    }

    @Test
    @DisplayName("2. @RequestParam opcjonalny: filtrowanie po kategorii i cenie maksymalnej, limit z wartości domyślnej")
    void requestParamFiltering() throws Exception {
        // bez parametrów: wszystkie 5 produktów (limit = domyślne 10)
        mockMvc.perform(get("/rest02/products")).andExpect(jsonPath("$.length()").value(5));

        // category + maxPrice razem: napoje do 20,00 zł → tylko Herbata (15,99)
        mockMvc.perform(get("/rest02/products").param("category", "napoje").param("maxPrice", "2000"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Herbata"));

        // limit zmienia wartość domyślną
        mockMvc.perform(get("/rest02/products").param("limit", "2")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("2. @RequestParam z listą: ?tag=bio&tag=import i ?tag=bio,import dają to samo")
    void requestParamList() throws Exception {
        // produkty z JAKIMKOLWIEK z podanych tagów: Kawa, Herbata, Czekolada, Kubek (Ciastka nie mają tagów)
        mockMvc.perform(get("/rest02/products").param("tag", "bio", "import"))   // powtórzony parametr
                .andExpect(jsonPath("$.length()").value(4));
        mockMvc.perform(get("/rest02/products?tag=bio,import"))                  // wartości po przecinku
                .andExpect(jsonPath("$.length()").value(4));
        mockMvc.perform(get("/rest02/products").param("tag", "bio"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("2. Parametr wymagany bez wartości → 400; zły typ liczby → 400; pusty napis to NIE brak parametru")
    void requestParamRequiredAndErrors() throws Exception {
        // search(@RequestParam String q) — q jest wymagany domyślnie
        mockMvc.perform(get("/rest02/products/search"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .hasMessageContaining("Required request parameter 'q'"));
        mockMvc.perform(get("/rest02/products/search").param("q", "ka"))
                .andExpect(jsonPath("$.length()").value(2));   // Kawa, Ciastka

        // limit=abc nie da się zamienić na int; to samo dla Optional<Long> maxPrice
        mockMvc.perform(get("/rest02/products").param("limit", "abc")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/rest02/products").param("maxPrice", "abc")).andExpect(status().isBadRequest());

        // ?category= (parametr jest, wartość pusta) to pusty napis, a nie null → filtr „kategoria = ''” nic nie znajduje
        mockMvc.perform(get("/rest02/products").param("category", ""))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("3. @RequestHeader: własny nagłówek wymagany (brak → 400), Accept-Language z wartością domyślną")
    void requestHeader() throws Exception {
        mockMvc.perform(get("/rest02/client/info").header("X-Shop-Client", "mobile"))
                .andExpect(content().string("klient=mobile, język=pl"));   // brak Accept-Language → domyślne „pl”

        mockMvc.perform(get("/rest02/client/info").header("X-Shop-Client", "mobile").header("Accept-Language", "en"))
                .andExpect(content().string("klient=mobile, język=en"));

        mockMvc.perform(get("/rest02/client/info"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .hasMessageContaining("Required request header 'X-Shop-Client'"));
    }

    @Test
    @DisplayName("3. @CookieValue: ciasteczko z zapytania, a gdy go brak (required = false) — null")
    void cookieValue() throws Exception {
        mockMvc.perform(get("/rest02/client/session").cookie(new MockCookie("shopSession", "abc123")))
                .andExpect(content().string("sesja=abc123"));
        mockMvc.perform(get("/rest02/client/session"))
                .andExpect(content().string("brak sesji"));
    }

    @Test
    @DisplayName("4. @RequestBody: JSON zamienia się na rekord; brakujące pole to null, nieznane pole jest pomijane")
    void requestBodyJson() throws Exception {
        mockMvc.perform(post("/rest02/reviews/json").contentType("application/json")
                        .content("{\"author\":\"Ola\",\"rating\":5,\"text\":\"Super kawa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Ola"))
                .andExpect(jsonPath("$.rating").value(5));

        // brak pól rating i text w JSON-ie → null w rekordzie (a w odpowiedzi: null w JSON-ie)
        mockMvc.perform(post("/rest02/reviews/json").contentType("application/json").content("{\"author\":\"Ola\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value((Object) null))
                .andExpect(jsonPath("$.text").value((Object) null));

        // pole, którego rekord nie ma, Boot po cichu pomija (domyślnie, więcej: Rest04Json)
        mockMvc.perform(post("/rest02/reviews/json").contentType("application/json")
                        .content("{\"author\":\"Ola\",\"zzz\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. Zły JSON, brak ciała albo zły typ pola → 400")
    void requestBodyErrors() throws Exception {
        // niepoprawny JSON (nazwa pola bez cudzysłowu)
        mockMvc.perform(post("/rest02/reviews/json").contentType("application/json").content("{author:"))
                .andExpect(status().isBadRequest());

        // brak ciała (@RequestBody jest domyślnie wymagane)
        mockMvc.perform(post("/rest02/reviews/json").contentType("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .hasMessageContaining("Required request body is missing"));

        // tekst tam, gdzie ma być liczba
        mockMvc.perform(post("/rest02/reviews/json").contentType("application/json").content("{\"rating\":\"dużo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Jackson 3: brak pola prymitywnego (int) w JSON-ie to 400, a nie po cichu 0")
    void primitiveFieldMissing() throws Exception {
        mockMvc.perform(post("/rest02/reviews/score").contentType("application/json").content("{\"points\":7}"))
                .andExpect(jsonPath("$.points").value(7));
        mockMvc.perform(post("/rest02/reviews/score").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .hasMessageContaining("FAIL_ON_NULL_FOR_PRIMITIVES"));
    }

    @Test
    @DisplayName("5. PUŁAPKA: formularz czytamy @RequestParam, JSON — @RequestBody; pomylenie daje 400 albo 415")
    void formVersusJson() throws Exception {
        // formularz + @RequestParam: działa
        mockMvc.perform(post("/rest02/reviews/form").param("author", "Ola").param("rating", "4"))
                .andExpect(content().string("formularz: Ola 4"));

        // JSON wysłany do metody z @RequestParam: parametrów w „zapytaniu” nie ma → 400
        mockMvc.perform(post("/rest02/reviews/form").contentType("application/json")
                        .content("{\"author\":\"Ola\",\"rating\":4}"))
                .andExpect(status().isBadRequest());

        // formularz wysłany do metody z @RequestBody (przyjmuje JSON) → 415
        mockMvc.perform(post("/rest02/reviews/json")
                        .contentType("application/x-www-form-urlencoded").content("author=Ola"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("5. PUŁAPKA: nazwa @PathVariable bez jawnej nazwy działa tylko dzięki kompilacji z -parameters")
    void parametersFlag() throws Exception {
        Method method = ProductController.class.getMethod("byId", Long.class);
        // isNamePresent() = „nazwa parametru jest zapisana w pliku .class”. Gdyby było false, Spring nie wiedziałby,
        // że parametr „id” ma wziąć zmienną {id}. Parent pom Spring Boota włącza flagę -parameters kompilatora.
        assertThat(method.getParameters()[0].isNamePresent()).isTrue();
        assertThat(method.getParameters()[0].getName()).isEqualTo("id");
    }
}
