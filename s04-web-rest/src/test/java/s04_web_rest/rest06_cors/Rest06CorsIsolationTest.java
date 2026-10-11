package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import s04_web_rest.rest01_controllers.Rest01Application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Sekcja 4 lekcji Rest06Cors — izolacja lekcji: startujemy aplikację lekcji Rest01 ({@code classes = Rest01Application})
 * i sprawdzamy, że CorsConfig z tej lekcji jej NIE dotyczy (pytanie 7). To jedyny test, który celowo sięga do innej
 * lekcji — żeby pokazać, że klasy startowe skanują tylko własne pakiety.
 */
@SpringBootTest(classes = Rest01Application.class)
@AutoConfigureMockMvc
class Rest06CorsIsolationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("4. Aplikacja lekcji Rest01 nie ma beana CorsConfig ani nagłówków CORS dla panelu")
    void corsConfigDoesNotLeakToOtherLessons() throws Exception {
        assertThat(context.getBeanNamesForType(CorsConfig.class)).isEmpty();

        mockMvc.perform(options("/api/products").header(HttpHeaders.ORIGIN, "https://panel.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
