package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Sekcja 4 lekcji Sec06SecurityTesting: MACIERZ dostępu — każdy punkt końcowy × każdy rodzaj użytkownika w jednej tabeli.
 * Jeden wiersz {@code @CsvSource} = jeden test. Nowy adres albo nowa rola = nowe wiersze, a nie nowa metoda.
 */
@WebMvcTest
@Import({Sec06SecurityConfig.class, OrderService.class})
class Sec06AccessMatrixTest {

    @Autowired
    MockMvcTester mvc;

    private static RequestPostProcessor who(String who) {
        return switch (who) {
            case "anonim" -> anonymous();
            case "anna" -> user("anna").roles("CUSTOMER");
            case "ewa" -> user("ewa").roles("EMPLOYEE");
            case "admin" -> user("admin").roles("ADMIN");
            default -> throw new IllegalArgumentException("Nieznany użytkownik w tabeli: " + who);
        };
    }

    @ParameterizedTest(name = "{0} {1} jako {2} → {3}")
    @CsvSource(textBlock = """
            # metoda, adres,                 kto,    status
            GET,    /sec06/products,         anonim, 200
            GET,    /sec06/products,         anna,   200
            POST,   /sec06/products,         anonim, 401
            POST,   /sec06/products,         anna,   403
            POST,   /sec06/products,         ewa,    201
            POST,   /sec06/products,         admin,  201
            GET,    /sec06/orders,           anonim, 401
            GET,    /sec06/orders,           anna,   200
            GET,    /sec06/orders/of/piotr,  anna,   403
            GET,    /sec06/orders/of/piotr,  ewa,    200
            GET,    /sec06/orders/of/piotr,  admin,  200
            DELETE, /sec06/orders/1,         anonim, 401
            DELETE, /sec06/orders/1,         ewa,    403
            DELETE, /sec06/orders/1,         admin,  200
            GET,    /sec06/admin/stats,      anonim, 401
            GET,    /sec06/admin/stats,      anna,   403
            GET,    /sec06/admin/stats,      ewa,    403
            GET,    /sec06/admin/stats,      admin,  200
            GET,    /sec06/internal/debug,   admin,  403
            GET,    /sec06/internal/debug,   anonim, 401
            GET,    /sec06/panel,            anna,   403
            GET,    /sec06/panel,            ewa,    200
            GET,    /sec06/panel,            admin,  200
            """)
    @DisplayName("Sekcja 4: macierz dostępu — punkt końcowy × użytkownik → oczekiwany status")
    void accessMatrix(String method, String path, String who, int expectedStatus) {
        assertThat(mvc.method(HttpMethod.valueOf(method)).uri(path).with(who(who)))
                .hasStatus(expectedStatus);
    }
}
