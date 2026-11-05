package s12_security.sec05_pitfalls.default_converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcja 4 lekcji Sec05Jwt — PUŁAPKA: twierdzenie {@code roles} w tokenie, a domyślny konwerter czyta tylko {@code scope}. */
@SpringBootTest
@AutoConfigureMockMvc
class Sec05DefaultConverterPitfallTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder encoder;

    private String adminToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("admin")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("ADMIN"))
                .claim("scope", "orders:read")
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    @DisplayName("Sekcja 4 (pułapka): token z roles: [ADMIN] nie przechodzi hasRole(\"ADMIN\") — 403; scope działa jako SCOPE_orders:read")
    void rolesClaimIgnoredByDefault() {
        String token = adminToken();
        assertThat(mvc.get().uri("/sec05/pitfall/admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/sec05/pitfall/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk();
    }
}
