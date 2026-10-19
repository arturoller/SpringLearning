package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest06Cors. MockMvc pozwala „udawać przeglądarkę”: dodajemy nagłówek Origin (pochodzenie strony)
 * i — dla zapytania wstępnego — Access-Control-Request-Method. Sprawdzamy, jakie nagłówki Access-Control-* odsyła
 * serwer. Pamiętaj: to PRZEGLĄDARKA na ich podstawie decyduje, czy skrypt zobaczy odpowiedź.
 * {@code @WebMvcTest} ładuje też CorsConfig, bo implementuje WebMvcConfigurer (ten typ wycinek webowy zawsze dołącza).
 */
@WebMvcTest(controllers = {
        CorsControllers.ShopController.class,
        CorsControllers.PublicController.class,
        CorsControllers.PartnerController.class,
        CorsControllers.InternalController.class})
class Rest06CorsTest {

    private static final String SHOP_FRONTEND = "http://localhost:3000";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. Bez nagłówka Origin (curl, plik .http, inny serwer) CORS w ogóle się nie włącza — zwykła odpowiedź")
    void noOriginNoCors() throws Exception {
        mockMvc.perform(get("/api/shop/products"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("2. Globalna konfiguracja: dozwolone pochodzenie → 200 + Access-Control-Allow-Origin i Expose-Headers")
    void globalConfigAllowedOrigin() throws Exception {
        mockMvc.perform(get("/api/shop/products").header(HttpHeaders.ORIGIN, SHOP_FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SHOP_FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Location, X-Total-Count"));
    }

    @Test
    @DisplayName("2. Niedozwolone pochodzenie → 403 i ciało „Invalid CORS request” — Spring odrzuca, metoda nie rusza")
    void globalConfigForeignOrigin() throws Exception {
        mockMvc.perform(get("/api/shop/products").header(HttpHeaders.ORIGIN, "http://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"))
                .andExpect(header().doesNotExist("X-Total-Count"));
    }

    @Test
    @DisplayName("3. Zapytanie wstępne (preflight, OPTIONS) dla PUT: 200 i nagłówki zgody (metody, nagłówki, max-age)")
    void preflightAllowed() throws Exception {
        mockMvc.perform(options("/api/shop/products")
                        .header(HttpHeaders.ORIGIN, SHOP_FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SHOP_FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "content-type"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    @DisplayName("3. Preflight z niedozwoloną metodą (PATCH) albo nagłówkiem (Authorization) → 403")
    void preflightRejected() throws Exception {
        mockMvc.perform(options("/api/shop/products")
                        .header(HttpHeaders.ORIGIN, SHOP_FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isForbidden());

        mockMvc.perform(options("/api/shop/products")
                        .header(HttpHeaders.ORIGIN, SHOP_FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. @CrossOrigin(origins = \"*\") na metodzie: każda strona; domyślnie max-age 1800 i tylko metoda z mapowania")
    void crossOriginAnnotationAnyOrigin() throws Exception {
        mockMvc.perform(get("/api/public/opening-hours").header(HttpHeaders.ORIGIN, "http://whatever.example"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));

        mockMvc.perform(options("/api/public/opening-hours")
                        .header(HttpHeaders.ORIGIN, "http://whatever.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800"));
    }

    @Test
    @DisplayName("5. originPatterns + allowCredentials: subdomena partnera dostaje SWOJE pochodzenie i Allow-Credentials: true")
    void originPatternsWithCredentials() throws Exception {
        mockMvc.perform(get("/api/partners/stock").header(HttpHeaders.ORIGIN, "https://hurt.partner.example"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://hurt.partner.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(get("/api/partners/stock").header(HttpHeaders.ORIGIN, "https://partner.example.evil.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Brak konfiguracji CORS: serwer ODDAJE dane (200), tylko bez Access-Control-Allow-Origin (pytanie 3)")
    void noCorsConfigServerStillResponds() throws Exception {
        mockMvc.perform(get("/api/internal/report").header(HttpHeaders.ORIGIN, SHOP_FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        // preflight też dostaje 200, ale bez nagłówków zgody — przeglądarka nie wyśle właściwego zapytania
        mockMvc.perform(options("/api/internal/report")
                        .header(HttpHeaders.ORIGIN, SHOP_FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
