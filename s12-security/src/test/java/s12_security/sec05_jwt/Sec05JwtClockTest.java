package s12_security.sec05_jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test sekcji 5 lekcji Sec05Jwt: czas tokenu liczony WSTRZYKNIĘTYM zegarem (Clock). Zatrzymany zegar ustawiony na
 * 1 stycznia 2020 — TokenService wystawia token „z 2020 roku”, a dekoder sprawdza go tym samym zegarem, więc token jest
 * ważny. Żadnego czekania i żadnej zależności od dzisiejszej daty.
 *
 * <p>{@code @TestConfiguration} (konfiguracja testowa) w klasie testu dokłada bean do kontekstu; {@code @Primary}
 * (główny) sprawia, że to ON zostanie wstrzyknięty zamiast Clock.systemUTC() z Sec05JwtConfig.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sec05JwtClockTest {

    static final Instant FIXED_NOW = Instant.parse("2020-01-01T10:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);   // fixed = zatrzymany: zawsze ta sama chwila
        }
    }

    @Autowired
    TokenService tokenService;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("5. Zatrzymany zegar: token „z 2020 roku” jest ważny, bo wystawca i dekoder używają tego samego Clock")
    void tokenTimeComesFromInjectedClock() throws Exception {
        // TestingAuthenticationToken = gotowe „uwierzytelnienie do testów” (login, hasło, uprawnienia)
        String token = tokenService.issue(new TestingAuthenticationToken("piotr", null, "ROLE_USER"));

        assertThat(jwtDecoder.decode(token).getIssuedAt()).isEqualTo(FIXED_NOW);
        assertThat(jwtDecoder.decode(token).getExpiresAt()).isEqualTo(Instant.parse("2020-01-01T11:00:00Z"));
        mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
}
