package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest04Json — część „przez HTTP”. Wycinek: {@code @WebMvcTest} + MockMvc, bo tu liczy się cała droga:
 * zapytanie → konwerter JSON w Spring MVC → rekord → odpowiedź. (Samo „jak wygląda JSON” sprawdza Rest04JsonMapperTest.)
 */
@WebMvcTest(JsonController.class)
class Rest04JsonTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. GET zwraca rekord jako application/json; POST odczytuje JSON z ciała na rekord")
    void recordInAndOut() throws Exception {
        mockMvc.perform(get("/rest04/products/sample"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("{\"id\":1,\"name\":\"Kawa\",\"price\":24.90,\"category\":\"DRINKS\"}"));

        mockMvc.perform(post("/rest04/products/echo").contentType("application/json")
                        .content("{\"id\":5,\"name\":\"Ciastka\",\"price\":12.50,\"category\":\"SWEETS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ciastka"))
                .andExpect(jsonPath("$.category").value("SWEETS"));
    }

    @Test
    @DisplayName("1. Zły JSON w ciele → 400 (jak w Rest02RequestData); niepoprawny typ pola → 400")
    void badJsonIs400() throws Exception {
        mockMvc.perform(post("/rest04/products/echo").contentType("application/json").content("{id:1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/rest04/products/echo").contentType("application/json")
                        .content("{\"id\":\"abc\",\"name\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("2. Odpowiedź GET dla klienta: full_name, brak passwordHash i brak phone (null), data jako dd.MM.yyyy")
    void customerAnnotationsInResponse() throws Exception {
        mockMvc.perform(get("/rest04/customers/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("Anna Nowak"))
                .andExpect(jsonPath("$.birthDate").value("17.05.1990"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    @DisplayName("2. @JsonIgnore działa też na wejściu: passwordHash z żądania NIE trafia do rekordu")
    void jsonIgnoreOnInput() throws Exception {
        mockMvc.perform(post("/rest04/customers/inspect").contentType("application/json")
                        .content("{\"id\":1,\"full_name\":\"Jan\",\"passwordHash\":\"wstrzyknięty\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("passwordHash=null"));
    }

    @Test
    @DisplayName("3. Daty w odpowiedzi: ISO-8601 jako tekst; zła data w żądaniu → 400")
    void datesOverHttp() throws Exception {
        mockMvc.perform(get("/rest04/orders/sample"))
                .andExpect(jsonPath("$.orderDate").value("2026-03-15"))
                .andExpect(jsonPath("$.placedAt").value("2026-03-15T10:15:30Z"))
                .andExpect(jsonPath("$.total").value(49.80))
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(post("/rest04/orders/echo").contentType("application/json")
                        .content("{\"id\":1,\"orderDate\":\"15-03-2026\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Nieznane pole w żądaniu jest ignorowane — 200, a odpowiedź nie zawiera tego pola")
    void unknownFieldIgnoredOverHttp() throws Exception {
        mockMvc.perform(post("/rest04/products/echo").contentType("application/json")
                        .content("{\"id\":1,\"name\":\"Kawa\",\"price\":24.90,\"category\":\"DRINKS\",\"color\":\"brązowy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kawa"))
                .andExpect(jsonPath("$.color").doesNotExist());
    }

    @Test
    @DisplayName("5. Nieznana stała enuma w żądaniu → 400; poprawna stała → 200")
    void unknownEnumIs400() throws Exception {
        mockMvc.perform(post("/rest04/products/echo").contentType("application/json")
                        .content("{\"id\":1,\"name\":\"Kawa\",\"category\":\"FRUIT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("1. Brak pola typu prostego (long id) → 400; pole opcjonalne bez wartości (name) → null")
    void missingPrimitiveIs400() throws Exception {
        mockMvc.perform(post("/rest04/products/echo").contentType("application/json").content("{\"name\":\"Kawa\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/rest04/products/echo").contentType("application/json").content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value((Object) null));
    }
}
