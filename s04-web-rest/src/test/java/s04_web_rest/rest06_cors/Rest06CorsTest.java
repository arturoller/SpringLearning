package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rest06Cors. MockMvc udaje przeglądarkę: dokłada nagłówki Origin i Access-Control-Request-*.
 * {@code @WebMvcTest} (bez listy) bierze wszystkie kontrolery lekcji ORAZ beany WebMvcConfigurer — czyli CorsConfig.
 */
@WebMvcTest
class Rest06CorsTest {

    static final String SHOP = "https://sklep.example.com";
    static final String PANEL = "https://panel.example.com";
    static final String THIEF = "https://zlodziej.example.com";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. Zapytanie bez Origin to nie CORS: 200 i żadnych nagłówków Access-Control-*")
    void noOriginIsNotCors() throws Exception {
        mockMvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("2. Brak konfiguracji CORS: obce Origin → serwer WYKONUJE zapytanie (200), ale nie daje zgody")
    void noCorsConfiguration() throws Exception {
        mockMvc.perform(get("/api/internal/stats").header(HttpHeaders.ORIGIN, THIEF))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"ordersToday\":42}"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mockMvc.perform(options("/api/internal/stats").header(HttpHeaders.ORIGIN, THIEF)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("3. @CrossOrigin: dozwolone pochodzenie → 200 i Access-Control-Allow-Origin")
    void crossOriginAllowed() throws Exception {
        mockMvc.perform(get("/api/catalog").header(HttpHeaders.ORIGIN, SHOP))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SHOP));
    }

    @Test
    @DisplayName("3. @CrossOrigin: obce pochodzenie → 403 „Invalid CORS request” (pytanie 3)")
    void crossOriginRejected() throws Exception {
        mockMvc.perform(get("/api/catalog").header(HttpHeaders.ORIGIN, THIEF))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"));
    }

    @Test
    @DisplayName("3. Preflight do @CrossOrigin: 200, Allow-Methods: GET, Max-Age: 1800, dowolny nagłówek zapytania")
    void crossOriginPreflight() throws Exception {
        mockMvc.perform(options("/api/catalog").header(HttpHeaders.ORIGIN, SHOP)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Anything"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SHOP))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "X-Anything"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800"));
    }

    @Test
    @DisplayName("3. Spring nie pozwala połączyć origins = \"*\" z allowCredentials(true)")
    void wildcardWithCredentialsRejected() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowCredentials(true);

        assertThatThrownBy(config::validateAllowCredentials).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("4. Konfiguracja globalna: preflight POST z Content-Type z panelu → 200 i pełny zestaw nagłówków zgody")
    void globalPreflightAllowed() throws Exception {
        mockMvc.perform(options("/api/orders").header(HttpHeaders.ORIGIN, PANEL)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PANEL))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
        mockMvc.perform(post("/api/orders").header(HttpHeaders.ORIGIN, PANEL)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"customer\":\"Anna\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PANEL))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Total-Count"));
    }

    @Test
    @DisplayName("4. Globalnie: DELETE spoza listy, nagłówek spoza listy, obce pochodzenie → 403 (pytanie 4)")
    void globalRejections() throws Exception {
        mockMvc.perform(options("/api/orders/5").header(HttpHeaders.ORIGIN, PANEL)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(options("/api/orders").header(HttpHeaders.ORIGIN, PANEL)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Secret"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders").header(HttpHeaders.ORIGIN, SHOP))
                .andExpect(status().isForbidden());
    }
}
