package s12_security.sec01_basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Sec01Basics — PUŁAPKA testów: {@code @WebMvcTest} BEZ {@code @Import(Sec01SecurityConfig.class)}.
 * Wycinek webowy ładuje kontrolery i beany typu {@code SecurityFilterChain}, ale NIE skanowane klasy {@code @Configuration}
 * — a nasz łańcuch jest metodą {@code @Bean} w takiej klasie. Zostaje domyślny łańcuch Boota: wszystko wymaga logowania.
 */
@WebMvcTest(Sec01Controller.class)
class Sec01SliceWithoutImportTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 3 (pułapka): bez @Import konfiguracji publiczny adres daje 401 — testujesz domyślne reguły Boota, nie swoje")
    void publicPathIsLockedWithoutImport() {
        assertThat(mvc.get().uri("/sec01/public/hello")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
