package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest06Cors. Wycinek: {@code @WebMvcTest} bez listy kontrolerów — bierze wszystkie kontrolery lekcji
 * ORAZ beany WebMvcConfigurer (nasz CorsConfig), bo CORS jest częścią warstwy web. Przeglądarki tu nie ma:
 * nagłówki, które normalnie dokłada przeglądarka (Origin, Access-Control-Request-Method), wpisujemy ręcznie.
 */
@WebMvcTest
class Rest06CorsTest {

    private static final String EVIL = "https://evil.example.com";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. Serwer nie blokuje: bez Origin i z obcym Origin odpowiedź to 200 — tylko bez nagłówka Access-Control-Allow-Origin")
    void serverDoesNotBlock() throws Exception {
        // tak pyta curl / HTTP Client — bez nagłówka Origin; CORS w ogóle nie wchodzi do gry
        mockMvc.perform(get("/rest06/plain/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("cześć"));

        // tak pyta przeglądarka ze strony evil.example.com; kontroler bez CORS WYKONAŁ SIĘ i odesłał dane,
        // ale brak Access-Control-Allow-Origin = przeglądarka nie pokaże odpowiedzi skryptowi
        mockMvc.perform(get("/rest06/plain/hello").header("Origin", EVIL))
                .andExpect(status().isOk())
                .andExpect(content().string("cześć"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("1. To samo pochodzenie (Origin = adres serwera) to NIE jest zapytanie CORS — reguły CORS nie są sprawdzane")
    void sameOriginIsNotCors() throws Exception {
        // MockMvc udaje serwer http://localhost; http://localhost NIE jest na liście allowedOrigins, a mimo to 200
        mockMvc.perform(get("/rest06/api/products").header("Origin", "http://localhost"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("2. Proste zapytanie z dozwolonego pochodzenia → 200 i nagłówki Access-Control-* w odpowiedzi")
    void simpleRequestFromAllowedOrigin() throws Exception {
        mockMvc.perform(get("/rest06/api/products").header("Origin", CorsConfig.DEV_FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Total-Count"))
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$[0].name").value("Kawa"));
    }

    @Test
    @DisplayName("2. Preflight (OPTIONS) → 200, dozwolone metody i nagłówki, Max-Age; kontroler się NIE wykonuje (puste ciało)")
    void preflightAllowed() throws Exception {
        mockMvc.perform(options("/rest06/api/products/1")
                        .header("Origin", CorsConfig.DEV_FRONTEND)
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"))
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("3. Globalna reguła: obce pochodzenie, metoda DELETE albo nagłówek spoza listy → preflight 403 „Invalid CORS request”")
    void preflightRejected() throws Exception {
        mockMvc.perform(options("/rest06/api/products/1")
                        .header("Origin", EVIL)
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mockMvc.perform(options("/rest06/api/products/1")
                        .header("Origin", CorsConfig.DEV_FRONTEND)
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isForbidden());

        mockMvc.perform(options("/rest06/api/products/1")
                        .header("Origin", CorsConfig.DEV_FRONTEND)
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Gdy ścieżka MA regułę CORS, Spring odrzuca też właściwe zapytanie spoza reguły → 403 (kontroler się nie wykona)")
    void actualRequestRejected() throws Exception {
        mockMvc.perform(get("/rest06/api/products").header("Origin", EVIL))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"));

        mockMvc.perform(delete("/rest06/api/products/1").header("Origin", CorsConfig.DEV_FRONTEND))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. @CrossOrigin: reguła z klasy działa dla każdej metody, a @CrossOrigin na metodzie DOKŁADA pochodzenia")
    void crossOriginAnnotation() throws Exception {
        mockMvc.perform(get("/rest06/partner/offers").header("Origin", "https://partner.example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://partner.example.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));   // domyślnie bez poświadczeń

        mockMvc.perform(get("/rest06/partner/offers").header("Origin", "https://blog.example.com"))
                .andExpect(status().isForbidden());

        // news: klasa (partner) + metoda (blog) → oba pochodzenia
        mockMvc.perform(get("/rest06/partner/news").header("Origin", "https://blog.example.com"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/rest06/partner/news").header("Origin", "https://partner.example.com"))
                .andExpect(status().isOk());

        mockMvc.perform(options("/rest06/partner/offers")
                        .header("Origin", "https://partner.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", "GET"))   // domyślnie: metody z mapowania
                .andExpect(header().string("Access-Control-Max-Age", "600"));
    }
}
