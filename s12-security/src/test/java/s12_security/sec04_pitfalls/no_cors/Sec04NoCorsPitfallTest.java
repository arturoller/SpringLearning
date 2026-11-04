package s12_security.sec04_pitfalls.no_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/** Sekcja 3 lekcji Sec04StatelessApi — PUŁAPKA: CORS tylko w Spring MVC, a nie w Spring Security. */
@SpringBootTest
@AutoConfigureMockMvc
class Sec04NoCorsPitfallTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 3 (pułapka): bez http.cors(...) preflight z dozwolonego pochodzenia dostaje 401 — przeglądarka zablokuje fetch")
    void preflightRejectedBySecurity() {
        assertThat(mvc.options().uri("/sec04/pitfall/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    @DisplayName("Sekcja 3 (pułapka): to samo żądanie GET z logowaniem działa i ma nagłówki CORS — dlatego curl i Postman \"nie widzą\" błędu")
    void actualRequestLooksFine() {
        assertThat(mvc.get().uri("/sec04/pitfall/orders").header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .with(httpBasic("anna", "haslo-testowe")))
                .hasStatusOk()
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000");
    }
}
