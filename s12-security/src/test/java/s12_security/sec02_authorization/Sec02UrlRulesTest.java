package s12_security.sec02_authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Sekcje 1, 2, 4 i 5 lekcji Sec02Authorization: reguły adresów. {@code @SpringBootTest} + {@code @AutoConfigureMockMvc}, bo
 * część adresów woła prawdziwy {@link OrderService} z bezpieczeństwem metod — z {@code @WebMvcTest} i {@code @MockitoBean}
 * serwis byłby atrapą bez proxy bezpieczeństwa. Logujemy się prawdziwym Basic ({@code httpBasic}), więc działa też
 * UserDetailsService lekcji i hierarchia ról.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sec02UrlRulesTest {

    @Autowired
    MockMvcTester mvc;

    private static final String NEW_PRODUCT = """
            {"name": "Czekolada gorzka", "price": 12.99}
            """;

    @Test
    @DisplayName("Sekcja 1: GET katalogu dla każdego; POST tylko EMPLOYEE (klient 403, anonim 401)")
    void methodSpecificRules() {
        assertThat(mvc.get().uri("/sec02/products")).hasStatusOk();
        assertThat(mvc.post().uri("/sec02/products").contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri("/sec02/products").contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT)
                .with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/sec02/products").contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT)
                .with(httpBasic("ewa", "haslo-testowe")))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("Sekcja 1: DELETE produktu tylko ADMIN — pracownik dostaje 403, mimo że może dodawać (POST)")
    void deleteOnlyAdmin() {
        assertThat(mvc.delete().uri("/sec02/products/1").with(httpBasic("ewa", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.delete().uri("/sec02/products/1").with(httpBasic("admin", "haslo-testowe")))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Sekcja 2: hasAuthority(\"REPORTS_READ\") — ewa ma to uprawnienie, admin nie (hierarchia dotyczy ról)")
    void authorityIsNotARole() {
        assertThat(mvc.get().uri("/sec02/reports/sales").with(httpBasic("ewa", "haslo-testowe"))).hasStatusOk();
        assertThat(mvc.get().uri("/sec02/reports/sales").with(httpBasic("admin", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Sekcja 4: hierarchia ról w regułach adresów — ADMIN wchodzi tam, gdzie wymagany jest EMPLOYEE i CUSTOMER")
    void roleHierarchyForUrls() {
        assertThat(mvc.post().uri("/sec02/products").contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT)
                .with(httpBasic("admin", "haslo-testowe")))
                .hasStatus(HttpStatus.CREATED);                                  // POST wymaga EMPLOYEE
        assertThat(mvc.get().uri("/sec02/orders").with(httpBasic("admin", "haslo-testowe"))).hasStatusOk();
        assertThat(mvc.get().uri("/sec02/admin/stats").with(httpBasic("ewa", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN);                                // hierarchia działa tylko "w dół"
    }

    @Test
    @DisplayName("Sekcja 3: przez HTTP odmowa z @PreAuthorize/@PostAuthorize to 403, a filtrowanie @PostFilter daje 200 z częścią danych")
    void methodSecurityOverHttp() {
        assertThat(mvc.get().uri("/sec02/orders/of/anna").with(httpBasic("anna", "haslo-testowe"))).hasStatusOk();
        assertThat(mvc.get().uri("/sec02/orders/of/piotr").with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec02/orders/3").with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN);                                // zamówienie piotra
        assertThat(mvc.get().uri("/sec02/orders").with(httpBasic("piotr", "haslo-testowe")))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].id").asArray().containsExactly(3);
    }

    @Test
    @DisplayName("Sekcja 5: deny by default — adres bez własnej reguły jest zamknięty nawet dla admina (403), anonim dostaje 401")
    void denyByDefault() {
        assertThat(mvc.get().uri("/sec02/internal/debug").with(httpBasic("admin", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec02/internal/debug")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
